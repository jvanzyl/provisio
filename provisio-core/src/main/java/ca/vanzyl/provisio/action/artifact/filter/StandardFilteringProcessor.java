/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.action.artifact.filter;

import ca.vanzyl.provisio.model.io.InterpolatingInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import io.tesla.proviso.archive.Selector;
import io.tesla.proviso.archive.UnarchivingEntryProcessor;

import static ca.vanzyl.provisio.ProvisioUtils.copy;

public class StandardFilteringProcessor implements UnarchivingEntryProcessor {

  Selector selector;
  Map<String, String> variables;

  public StandardFilteringProcessor(Map<String, String> variables) {
    this.variables = variables;
  }

  @Override
  public String processName(String name) {
    return name;
  }

  @Override
  public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
    copy(new InterpolatingInputStream(inputStream, variables), outputStream);
  }
}