/*
 *
 *  Copyright 2024 Alan Littleford
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package com.mentalresonance.dust.demos.dustville

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import groovy.util.logging.Slf4j

@Slf4j
class WebServer extends NanoHTTPD {
	WebServer(int port) {
		super(port)
		start(SOCKET_READ_TIMEOUT, false);
	}

	@Override
	Response serve(IHTTPSession session) {
		if (session.method.name() == 'GET') {
			if (session.uri.startsWith('/cars/road')) {
				String name = session.uri.split('/')[-1]
				log.info "$name"
				return Response(Status.OK)
			}
			else {
				String path = session.uri[1..-1] // Drop '/'

				String mimeType = URLConnection.guessContentTypeFromName(path)
				InputStream is = getClass().getClassLoader().getResourceAsStream(path)
				return newChunkedResponse(Status.OK, mimeType, is)
			}
		} else
			return Response(Status.BAD_REQUEST)
	}
}
