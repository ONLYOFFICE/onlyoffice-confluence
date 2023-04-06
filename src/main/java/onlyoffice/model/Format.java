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

package onlyoffice.model;

import java.util.List;

public class Format {
    private String name;
    private Type type;
    private boolean edit;
    private boolean fill;
    private boolean editable;
    private List<String> convert;
    private String mime;

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isEdit() {
        return edit;
    }

    public boolean isFill() {
        return fill;
    }

    public boolean isEditable() {
        return editable;
    }

    public List<String> getConvert() {
        return convert;
    }

    public String getMime() {
        return mime;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setEdit(final boolean edit) {
        this.edit = edit;
    }

    public void setFill(final boolean fill) {
        this.fill = fill;
    }

    public void setEditable(final boolean editable) {
        this.editable = editable;
    }

    public void setConvert(final List<String> convert) {
        this.convert = convert;
    }

    public void setMime(final String mime) {
        this.mime = mime;
    }

}
