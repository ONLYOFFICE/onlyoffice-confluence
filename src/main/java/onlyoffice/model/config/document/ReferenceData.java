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

package onlyoffice.model.config.document;

public class ReferenceData {
    private long fileKey;
    private String instanceId;

    public ReferenceData(final long fileKey, final String instanceId) {
        this.fileKey = fileKey;
        this.instanceId = instanceId;
    }

    public long getFileKey() {
        return fileKey;
    }

    public void setFileKey(final long fileKey) {
        this.fileKey = fileKey;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(final String instanceId) {
        this.instanceId = instanceId;
    }
}
