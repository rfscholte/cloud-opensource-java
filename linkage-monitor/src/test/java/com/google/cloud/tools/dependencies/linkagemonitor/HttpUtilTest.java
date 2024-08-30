/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.tools.dependencies.linkagemonitor;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

import static java.util.Map.entry;

public class HttpUtilTest {

  private static final Map<String, String> RANDOM_PARAMETERS = Map.ofEntries(
	  entry("v", "1"),
	  entry("tid", "UA-12345678-1"),
	  entry("ni", "0"),
	  entry("t", "pageview"),
	  entry("cd21", "1"),
	  entry("cd16", "0"),
	  entry("cd17", "0"),
	  entry("cid", "bee5d838-c3f8-4940-a944-b56973597e74"),
	  entry("cd19", "some-event-type"),
      entry("cd20", "some-event-name"),
      entry("dh", "virtual.host"),
      entry("dp", "/virtual/some-event-type/some-event-name"),
      entry("dt", "some-custom-key=some-custom-value")
  );

  private static final Map<String, String> ENCODED_PARAMETERS = Map.ofEntries(
		entry("dt", "some-custom-key%3Dsome-custom-value"),
		entry("cd16", "0"),
		entry("cd17", "0"),
		entry("v", "1"),
		entry("t", "pageview"),
		entry("cd21", "1"),
		entry("cd20", "some-event-name"),
		entry("ni", "0"),
		entry("tid", "UA-12345678-1"),
		entry("dh", "virtual.host"),
		entry("dp", "%2Fvirtual%2Fsome-event-type%2Fsome-event-name"),
		entry("cid", "bee5d838-c3f8-4940-a944-b56973597e74"),
		entry("cd19", "some-event-type")
  );

  @Test
  public void testGetParametersString() {
    String urlEncodedParameters = HttpUtil.getParametersString(RANDOM_PARAMETERS);

    String[] keyValuePairs = urlEncodedParameters.split("&");
    Assert.assertEquals(keyValuePairs.length, RANDOM_PARAMETERS.size());

    for (String pair : keyValuePairs) {
      String[] keyValue = pair.split("=");
      Assert.assertEquals(2, keyValue.length);
      Assert.assertEquals(keyValue[1], ENCODED_PARAMETERS.get(keyValue[0]));
    }
  }

  @Test
  public void testGetParametersString_percentEscaping() {
    Map<String, String> noEscape = new HashMap<>();
    noEscape.put("k", ".*-_abcXYZ");
    Assert.assertEquals("k=.*-_abcXYZ", HttpUtil.getParametersString(noEscape));

    Map<String, String> escape = new HashMap<>();
    escape.put("k", " ü한글+=,`~!@#$%^&()?<>{}][|:;/\\'\"");
    Assert.assertEquals("k=+%C3%BC%ED%95%9C%EA%B8%80%2B%3D%2C%60%7E%21%40%23"
        + "%24%25%5E%26%28%29%3F%3C%3E%7B%7D%5D%5B%7C%3A%3B%2F%5C%27%22",
        HttpUtil.getParametersString(escape));
  }
}
