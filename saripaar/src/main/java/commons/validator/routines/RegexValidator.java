/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package commons.validator.routines;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * <b>Regular Expression</b> validation (using JDK 1.4+ regex support).
 * <p>
 * Construct the validator either for a single regular expression or a set (array) of
 * regular expressions. By default validation is <i>case sensitive</i> but constructors
 * are provided to allow  <i>case in-sensitive</i> validation. For example to create
 * a validator which does <i>case in-sensitive</i> validation for a set of regular
 * expressions:
 * </p>
 * <pre>
 * <code>
 * String[] regexs = new String[] {...};
 * RegexValidator validator = new RegexValidator(regexs, false);
 * </code>
 * </pre>
 *
 * <ul>
 *   <li>Validate <code>true</code> or <code>false</code>:</li>
 *   <li>
 *     <ul>
 *       <li><code>boolean valid = validator.isValid(value);</code></li>
 *     </ul>
 *   </li>
 *   <li>Validate returning an aggregated String of the matched groups:</li>
 *   <li>
 *     <ul>
 *       <li><code>String result = validator.validate(value);</code></li>
 *     </ul>
 *   </li>
 *   <li>Validate returning the matched groups:</li>
 *   <li>
 *     <ul>
 *       <li><code>String[] result = validator.match(value);</code></li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>
 * Cached instances pre-compile and re-use {@link Pattern}(s) - which according
 * to the {@link Pattern} API are safe to use in a multi-threaded environment.
 * </p>
 *
 * @version $Revision$
 * @since Validator 1.4
 */
public class RegexValidator implements Serializable {
    private static final long serialVersionUID = -8832409930574867162L;
    private final Pattern[] patterns;

    public RegexValidator(String regex) {
        this(regex, true);
    }

    public RegexValidator(String regex, boolean caseSensitive) {
        this(new String[]{regex}, caseSensitive);
    }

    public RegexValidator(String[] regexs) {
        this(regexs, true);
    }

    public RegexValidator(String[] regexs, boolean caseSensitive) {
        if (regexs != null && regexs.length != 0) {
            this.patterns = new Pattern[regexs.length];
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;

            for(int i = 0; i < regexs.length; ++i) {
                if (regexs[i] == null || regexs[i].length() == 0) {
                    throw new IllegalArgumentException("Regular expression[" + i + "] is missing");
                }

                this.patterns[i] = Pattern.compile(regexs[i], flags);
            }

        } else {
            throw new IllegalArgumentException("Regular expressions are missing");
        }
    }

    public boolean isValid(String value) {
        if (value == null) {
            return false;
        } else {
            for(int i = 0; i < this.patterns.length; ++i) {
                if (this.patterns[i].matcher(value).matches()) {
                    return true;
                }
            }

            return false;
        }
    }

    public String[] match(String value) {
        if (value == null) {
            return null;
        } else {
            for(int i = 0; i < this.patterns.length; ++i) {
                Matcher matcher = this.patterns[i].matcher(value);
                if (matcher.matches()) {
                    int count = matcher.groupCount();
                    String[] groups = new String[count];

                    for(int j = 0; j < count; ++j) {
                        groups[j] = matcher.group(j + 1);
                    }

                    return groups;
                }
            }

            return null;
        }
    }

    public String validate(String value) {
        if (value == null) {
            return null;
        } else {
            for(int i = 0; i < this.patterns.length; ++i) {
                Matcher matcher = this.patterns[i].matcher(value);
                if (matcher.matches()) {
                    return value;
                }
            }

            return null;
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("RegexValidator{");

        for(int i = 0; i < this.patterns.length; ++i) {
            if (i > 0) {
                buffer.append(",");
            }

            buffer.append(this.patterns[i].pattern());
        }

        buffer.append("}");
        return buffer.toString();
    }
}
