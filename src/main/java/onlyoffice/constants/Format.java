/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

package onlyoffice.constants;

import java.util.List;

public class Format {
    public String name;
    public Type type;
    public boolean edit;
    public boolean fillForm;
    public List<String> convertTo;

    public Format(String name, Type type, List<String> convertTo) {
        this.name = name;
        this.type = type;
        this.edit = false;
        this.fillForm = false;
        this.convertTo = convertTo;
    }

    public Format(String name, Type type, boolean edit, List<String> convertTo) {
        this.name = name;
        this.type = type;
        this.edit = edit;
        this.fillForm = false;
        this.convertTo = convertTo;
    }

    public Format(String name, Type type, boolean edit, boolean fillForm, List<String> convertTo) {
        this.name = name;
        this.type = type;
        this.edit = edit;
        this.fillForm = fillForm;
        this.convertTo = convertTo;
    }

    public String getName() {
        return name;
    }

    public Type getType() { return type; }

    public boolean isEdit() { return edit; }

    public boolean isFillForm() { return fillForm; }

    public List<String> getConvertTo() {
        return convertTo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) { this.type = type; }

    public void setConvertTo(List<String> convertTo) {
        this.convertTo = convertTo;
    }

    public void setEdit(boolean edit) { this.edit = edit; }

    public void setFillForm(boolean fillForm) { this.fillForm = fillForm; }

}