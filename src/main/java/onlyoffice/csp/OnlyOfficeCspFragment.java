/**
 *
 * (c) Copyright Ascensio System SIA 2026
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

package onlyoffice.csp;

import com.atlassian.security.csp.api.CspDirective;
import com.atlassian.security.csp.api.CspFragment;
import onlyoffice.sdk.manager.url.UrlManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

/**
 * Allows the Document Server API script (api.js) in the script-src CSP directive.
 */
public class OnlyOfficeCspFragment implements CspFragment {
    private final Logger log = LoggerFactory.getLogger("onlyoffice.csp.OnlyOfficeCspFragment");

    private final UrlManager urlManager;

    public OnlyOfficeCspFragment(final UrlManager urlManager) {
        this.urlManager = urlManager;
    }

    @Override
    public Set<CspDirective> getCSPDirectives() {
        return Collections.singleton(CspDirective.SCRIPT_SRC);
    }

    @Override
    public Set<URI> getCSPOrigins(final CspDirective cspDirective) {
        if (!CspDirective.SCRIPT_SRC.equals(cspDirective)) {
            return Collections.emptySet();
        }

        URI origin = getDocumentServerOrigin();

        return origin == null ? Collections.emptySet() : Collections.singleton(origin);
    }

    @Override
    public Set<String> getUrlPatterns() {
        return Collections.singleton("/**");
    }

    private URI getDocumentServerOrigin() {
        String documentServerUrl = urlManager.getDocumentServerUrl();

        if (documentServerUrl == null || documentServerUrl.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(documentServerUrl);

            // A relative Document Server URL is served from the Confluence origin, which 'self' already covers
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            log.warn("Invalid Document Server URL: " + documentServerUrl, e);
            return null;
        }
    }
}
