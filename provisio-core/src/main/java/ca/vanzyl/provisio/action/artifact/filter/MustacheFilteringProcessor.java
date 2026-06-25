/*
 * Copyright (C) 2015-2024 Jason van Zyl
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

import ca.vanzyl.provisio.archive.UnarchivingEnhancedEntryProcessor;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class MustacheFilteringProcessor implements UnarchivingEnhancedEntryProcessor {

    private final Map<String, Object> variables;

    public MustacheFilteringProcessor(Map<String, String> variables) {
        this.variables = mustacheVariables(variables);
    }

    @Override
    public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream);
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new InputStreamReader(inputStream), "provisio");
        mustache.execute(writer, variables);
        writer.flush();
    }

    private Map<String, Object> mustacheVariables(Map<String, String> variables) {
        Map<String, Object> mustacheVariables = new HashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue();
            if ("true".equalsIgnoreCase(value)) {
                mustacheVariables.put(entry.getKey(), Boolean.TRUE);
            } else if ("false".equalsIgnoreCase(value)) {
                mustacheVariables.put(entry.getKey(), Boolean.FALSE);
            } else {
                mustacheVariables.put(entry.getKey(), value);
            }
        }
        return mustacheVariables;
    }
}
