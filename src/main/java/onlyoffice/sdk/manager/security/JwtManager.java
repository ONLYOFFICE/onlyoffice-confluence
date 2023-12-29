/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
 *
 */

package onlyoffice.sdk.manager.security;

import java.util.Map;

public interface JwtManager extends com.onlyoffice.manager.security.JwtManager {
    String createInternalToken(Map<String, ?> payloadMap);
    String verifyInternalToken(String token);
    String createHash(String str);
    String readHash(String base64);
}