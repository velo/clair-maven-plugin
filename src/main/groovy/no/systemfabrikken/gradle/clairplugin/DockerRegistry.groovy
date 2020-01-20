/**
 * Copyright (C) 2020 Marvin Herman Froeder (marvin@marvinformatics.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.systemfabrikken.gradle.clairplugin

import com.mashape.unirest.http.Unirest
import groovy.json.JsonSlurper

public class DockerRegistry {

    private final String host
    private final int port

    private final JsonSlurper jsonSlurper

    public DockerRegistry(host, port) {
        this.host = Objects.requireNonNull(host, "host for registry must be provided")
        this.port = port

        this.jsonSlurper = new JsonSlurper()
    }

    public ImageManifest getManifest(String repo, String tag) {
        def manifestUrl = "https://${host}:${port}/v2/${repo}/manifests/${tag}"
        def req = Unirest.get(manifestUrl).asString()

        if (req.status != 200) throw new RuntimeException("Could not download manifest from URL: ${manifestUrl}")
        def json = jsonSlurper.parseText(req.body)

        return new ImageManifest(repo, tag, json)
    }

    public String blobPath(ImageManifest manifest, Layer layer) {
        return "https://${this.host}:${this.port}/v2/${manifest.repo}/blobs/${layer.blobsum}"
    }

    public static class ImageManifest {

        private final List<Layer> layers
        private final Layer parentLayer
        private final String tag
        private final String repo


        ImageManifest(String repo, String tag, Object json) {
            this.tag = tag
            this.repo = repo

            def layersBlobsums = json.fsLayers.collect{it.blobSum}.reverse()
            def history = json.history.reverse()

            this.layers = layersBlobsums.withIndex().collect { String blobSum, int index ->
                if(index > 0) return new Layer(blobsum: blobSum, parent: layersBlobsums[index-1], name: "${repoNormalized()}:${tag}:${blobSumRaw(blobSum)}", history: history[index])
                return new Layer(blobsum: blobSum, name: "${repoNormalized()}:${tag}", history: history[index])
            }

            this.parentLayer = this.layers.find { it.parent == null}
        }

        public static String blobSumRaw(String blobsum) {
            return blobsum.replace('sha256:', '')
        }

        public String repoNormalized() {
            // Clair is not fond of / in repo names since it is part of the URL
            return this.repo.replace('/', ':')
        }

        public Layer lastLayer() {
            return this.layers.last()
        }
    }

    public static class Layer {
        private String name
        private String blobsum
        private String parent // assumed as previous layer when iteraring through the layers
        private String history

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Layer{")
            sb.append("name='").append(name).append('\'')
            sb.append(", blobsum='").append(blobsum).append('\'')
            sb.append(", parent='").append(parent).append('\'')
            sb.append('}')
            return sb.toString()
        }
    }
}

