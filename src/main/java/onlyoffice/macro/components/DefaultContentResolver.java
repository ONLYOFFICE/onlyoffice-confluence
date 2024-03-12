/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

package onlyoffice.macro.components;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.Comment;
import com.atlassian.confluence.pages.Draft;
import com.atlassian.confluence.pages.PageManager;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DefaultContentResolver implements ContentResolver {
    private final PageManager pageManager;

    public DefaultContentResolver(final PageManager pageManager) {
        this.pageManager = pageManager;
    }

    @Override
    public ContentEntityObject getContent(final String page, final String sk, final String date,
                                          final ContentEntityObject context) throws MacroExecutionException {
        ContentEntityObject content = null;
        String spaceKey = sk;

        try {
            if (StringUtils.isBlank((CharSequence) page)) {
                return context;
            }
            if (StringUtils.isBlank((CharSequence) spaceKey)) {
                spaceKey = this.getSpaceKey(context);
            }
            if (StringUtils.isBlank((CharSequence) spaceKey)) {
                throw new IllegalArgumentException(
                        "No spaceKey parameter was supplied and it could not be deduced from the context parameter."
                );
            }

            if (StringUtils.isNotBlank((CharSequence) date)) {
                final DateFormat dateFormat = DateFormat.getDateInstance(3, Locale.US);
                final Date parsedDate = dateFormat.parse(date);
                final Calendar cal = Calendar.getInstance();
                cal.setTime(parsedDate);
                content = (ContentEntityObject) this.pageManager.getBlogPost(spaceKey, page, cal);
            } else {
                content = (ContentEntityObject) this.pageManager.getPage(spaceKey, page);
            }
        } catch (ParseException ex) {
            throw new MacroExecutionException("Unrecognized date string, please use mm/dd/yyyy");
        } catch (IllegalArgumentException ex2) {
            throw new MacroExecutionException("The space key could not be found.");
        }

        if (content == null) {
            throw new MacroExecutionException("The viewfile macro is unable to locate the page \""
                    + page + "\" in space \"" + spaceKey + "\"");
        }

        return content;
    }

    private String getSpaceKey(final ContentEntityObject contentObject) {
        if (contentObject == null) {
            return null;
        }

        ContentEntityObject ceo = contentObject;

        String spaceKey = null;
        if (ceo instanceof Comment) {
            ceo = ((Comment) ceo).getContainer();
        }

        if (ceo instanceof SpaceContentEntityObject) {
            spaceKey = ((SpaceContentEntityObject) ceo).getSpaceKey();
        } else if (ceo instanceof Draft) {
            spaceKey = ((Draft) ceo).getDraftSpaceKey();
        }
        return spaceKey;
    }
}
