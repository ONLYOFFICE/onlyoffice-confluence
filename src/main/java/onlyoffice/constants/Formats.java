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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Formats {
    public static final List<Format> formats = new ArrayList<Format>() {{
        add(new Format("djvu", Type.WORD, new ArrayList<String>()));
        add(new Format("doc", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("docm", Type.WORD, Arrays.asList("docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("docx", Type.WORD, true, Arrays.asList("docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("docxf", Type.FORM, true, Arrays.asList("docx", "oform", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("oform", Type.FORM, false, true, Arrays.asList( "pdf")));
        add(new Format("dot", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("dotm", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("dotx", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("epub", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("fb2", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("fodt", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("html", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("mht", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("odt", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("ott", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("pdf", Type.WORD, new ArrayList<>()));
        add(new Format("rtf", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "txt")));
        add(new Format("txt", Type.WORD, new ArrayList<>()));
        add(new Format("xps", Type.WORD, Arrays.asList("pdf", "pdfa")));
        add(new Format("oxps", Type.WORD, Arrays.asList("pdf", "pdfa")));
        add(new Format("xml", Type.WORD, Arrays.asList("docx", "docxf", "docm", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));

        add(new Format("csv", Type.CELL, new ArrayList<>()));
        add(new Format("fods", Type.CELL, Arrays.asList( "xlsx", "csv", "ods", "ots", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("ods", Type.CELL, Arrays.asList("xlsx", "csv", "ots", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("ots", Type.CELL, Arrays.asList("xlsx", "csv", "ods", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("xls", Type.CELL, Arrays.asList("xlsx", "csv", "ods", "ots", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("xlsm", Type.CELL, Arrays.asList("xlsx", "csv", "ods", "ots", "pdf", "pdfa", "xltx", "xltm")));
        add(new Format("xlsx", Type.CELL, true, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("xlt", Type.CELL, Arrays.asList("xlsx", "csv", "ods", "ots", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("xltm", Type.CELL, Arrays.asList("xlsx", "csv", "ods", "ots", "pdf", "pdfa", "xltx", "xlsm")));
        add(new Format("xltx", Type.CELL, Arrays.asList("xlsx", "csv", "ods", "ots", "pdf", "pdfa", "xlsm", "xltm")));

        add(new Format("fodp", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("odp", Type.SLIDE, Arrays.asList("pptx", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("otp", Type.SLIDE, Arrays.asList("pptx", "odp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("pot", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("potm", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm")));
        add(new Format("potx", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "pptm", "potm")));
        add(new Format("pps", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("ppsm", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("ppsx", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("ppt", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
        add(new Format("pptm", Type.SLIDE, Arrays.asList("pptx", "odp", "otp", "pdf", "pdfa", "potx", "potm")));
        add(new Format("pptx", Type.SLIDE, true, Arrays.asList( "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
    }};

    public static List<Format> getSupportedFormats() {
        return formats;
    }
}
