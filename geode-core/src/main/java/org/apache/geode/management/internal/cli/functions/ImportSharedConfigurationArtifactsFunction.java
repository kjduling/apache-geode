/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management.internal.cli.functions;

import org.apache.commons.io.FileUtils;
import org.apache.geode.cache.execute.FunctionAdapter;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.distributed.internal.SharedConfiguration;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.configuration.utils.ZipUtils;

import java.io.File;

/******
 * This function copies the zipped shared configuration, renames the existing shared configuration
 * directory and unzips the shared configuration.
 *
 */
public class ImportSharedConfigurationArtifactsFunction extends FunctionAdapter
    implements InternalEntity {

  private static final long serialVersionUID = 1L;

  @Override
  public void execute(FunctionContext context) {
    Object[] args = (Object[]) context.getArguments();
    String zipFileName = (String) args[0];
    byte[] zipFileData = (byte[]) args[1];

    InternalLocator locator = InternalLocator.getLocator();
    String memberName = locator.getDistributedSystem().getName();

    if (locator.isSharedConfigurationRunning()) {
      File zippedSharedConfiguration = new File(zipFileName);

      try {
        SharedConfiguration sc = locator.getSharedConfiguration();
        sc.renameExistingSharedConfigDirectory();
        sc.clearSharedConfiguration();
        FileUtils.writeByteArrayToFile(zippedSharedConfiguration, zipFileData);
        ZipUtils.unzip(zipFileName, sc.getSharedConfigurationDirPath());

        CliFunctionResult cliFunctionResult = new CliFunctionResult(memberName, true,
            CliStrings.IMPORT_SHARED_CONFIG__ARTIFACTS__COPIED);
        context.getResultSender().lastResult(cliFunctionResult);
      } catch (Exception e) {
        CliFunctionResult result =
            new CliFunctionResult(memberName, e, CliUtil.stackTraceAsString(e));
        context.getResultSender().lastResult(result);
      } finally {
        FileUtils.deleteQuietly(zippedSharedConfiguration);
      }
    } else {
      CliFunctionResult cliFunctionResult =
          new CliFunctionResult(memberName, false, CliStrings.SHARED_CONFIGURATION_NOT_STARTED);
      context.getResultSender().lastResult(cliFunctionResult);
    }
  }

  @Override
  public String getId() {
    return ImportSharedConfigurationArtifactsFunction.class.getName();
  }
}
