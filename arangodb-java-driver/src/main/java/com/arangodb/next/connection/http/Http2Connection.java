/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.next.connection.http;


import com.arangodb.next.connection.AuthenticationMethod;
import com.arangodb.next.connection.ConnectionConfig;
import com.arangodb.next.connection.HostDescription;
import reactor.netty.http.HttpProtocol;

import javax.annotation.Nullable;

/**
 * @author Michele Rastelli
 */
public final class Http2Connection extends HttpConnection {

    public Http2Connection(final HostDescription hostDescription,
                           @Nullable final AuthenticationMethod authenticationMethod,
                           final ConnectionConfig connectionConfig) {
        super(hostDescription, authenticationMethod, connectionConfig);
    }

    @Override
    protected HttpProtocol getProtocol() {
        if (getConfig().getUseSsl()) {
            return HttpProtocol.H2;
        } else {
            return HttpProtocol.H2C;
        }
    }

}
