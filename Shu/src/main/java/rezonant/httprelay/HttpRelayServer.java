/**
 * Copyright (c) 2014 William Lahti.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package rezonant.httprelay;

import android.util.ArrayMap;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * A class which uses NanoHTTPD to expose a streaming relay server. This can be paired with the
 * Android MediaPlayer APIs to allow streaming playback of resources over any protocol.
 *
 * The key integration point for users of this class is the StreamableResource interface.
 * Users register streamable resources by calling serveStream() and passing an instance of
 * the StreamableResource interface. This method will return a URL which represents the newly
 * registered stream resource.
 *
 * For as long as a resource is registered and the relay server is active, requests for
 * corresponding URLs will be handled by calling open() on the appropriate StreamableResource
 * with the requested offset to start the stream at. The open() method should then return an
 * InputStream which contains the data stream from the resource.
 *
 * The offset provided to open() is used to facilitate HTTP Range requests. While HttpRelayServer
 * does support range-less GET requests, streaming the whole file out to the HTTP response body,
 * the primary and most important request supported is a Range'd GET.
 *
 * All other HTTP request methods (POST, DELETE, etc) will result in an 403 Forbidden response and
 * are completely unsupported.
 *
 * All URLs other than those explicitly returned by serveResource() will result in a 404 Not Found
 * status and are completely unsupported.
 *
 * TODO: Remove or replace all references to Android's Log class before general release
 *
 * Created by liam on 7/28/14.
 */
public class HttpRelayServer {
    /**
     * Construct an HTTP relay server with the given hostname and IP address. The hostname is used
     * to construct URLs passed back from serveStream(). The IP address should be one of the
     * IP addresses assigned to a network interface on this machine, "127.0.0.1" for
     * loopback-only service, or "0.0.0.0" for service on all interfaces.
     *
     * @param hostname The hostname to use for URLs to this service. Used when construcitng
     *                 URLs for resources in serveStream()
     * @param ipAddress an IP address assigned to this machine, "127.0.0.1" for loopback-only, or
     *                  "0.0.0.0" for service on all interfaces.
     */
    public HttpRelayServer(String hostname, String ipAddress, int port)
    {
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Constructs a new local-only (localhost/127.0.0.1) HTTP service. If you use this constructor,
     * the web server will be made available only to other applications on the device the server
     * is running on. To instead allow external uses,
     */
    public HttpRelayServer()
    {
        this("localhost", "0.0.0.0", 9876);
    }

    private static HttpRelayServer theInstance = null;
    private String hostname;
    private String ipAddress;
    private int port;

    public String getHostname()
    {
        return hostname;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }


    /**
     * Retrieve a singleton instance of this
     * @return
     */
    public static HttpRelayServer instance() throws IOException {

        if (theInstance == null) {
            Log.i("FYI", "//////////// HTTP Relay static instance construction invoked ////////////////////");
            theInstance = new HttpRelayServer();
            theInstance.start();
        }

        return theInstance;
    }

    private Map<Long, StreamableResource> streams = new HashMap<Long,StreamableResource>();
    private long nextStreamId = 1337;

    /**
     * Registers an InputStream for HTTP relay streaming, returning the URL which can be
     * used to access the given stream.
     * @return A URL which can be used to access the resource
     */
    public String serveStream(StreamableResource stream)
    {
        long id = nextStreamId++;
        streams.put(id, stream);

        return "http://"+hostname+":"+port+"/stream/"+id;
    }

    /**
     * Decorate the given response with headers which are common to all requests, such as
     * additional HTTP features supported by this server. The default version simply advertises
     * support for Range headers in byte units (Accept-Ranges: bytes).
     *
     * You may extend this to expose more features.
     *
     * @param response
     * @return
     */
    protected NanoHTTPD.Response standardize(NanoHTTPD.Response response)
    {
        response.setChunkedTransfer(true);
        response.addHeader("Accept-Ranges", "bytes");
        return response;
    }

    public int getPort() {
        return port;
    }

    /**
     * A subclass of NanoHTTPD which accomplishes on-demand streaming of StreamableResource objects
     * as registered with HttpRelayServer.serveStream().
     *
     * Can be extended for other purposes as well.
     */
    public static class HTTPD extends NanoHTTPD {
        public HTTPD(HttpRelayServer server, String hostname, int port)
        {
            super(hostname, port);

            this.server = server;
        }

        private HttpRelayServer server;

        /**
         * Constructs an error response to be sent to the client with the given status code.
         * Can be overridden to capture or modify error responses as needed.
         *
         * @param status The status to be returned to the client
         * @return
         */
        protected Response error(Response.IStatus status, IHTTPSession session)
        {
            Log.e("FYI", "Error response for "+session.getUri()+
                    ": "+status.getRequestStatus()+" "+status.getDescription());

            return standardize(new Response(Response.Status.NOT_FOUND, "text/plain",
                    status.getRequestStatus()+" "+status.getDescription()));
        }

        /**
         * Apply common
         * @param response
         * @return
         */
        protected Response standardize(Response response)
        {
            Log.i("FYI", "Standardizing response with status: "+response.getStatus());
            return this.getServer().standardize(response);
        }

        @Override
        public Response serve(IHTTPSession session) {

            Log.i("FYI", "Request came in for "+session.getUri());

            // Tell users they are not allowed to run any requests except GET.

            if (!session.getMethod().equals(Method.GET))
                return error(Response.Status.UNAUTHORIZED, session);

            // Identify requested stream

            String uri = session.getUri().replaceAll("^/|/$", "");
            String[] parts = uri.split("/", 2);

            if (parts.length < 2) {
                Log.w("FYI", "Request was not recognized for component length in for "+session.getUri());
                return error(Response.Status.NOT_FOUND, session);
            }

            String type = parts[0];
            String ident = parts[1];
            long id = 0;

            try {
                id = Long.parseLong(ident);
            } catch (NumberFormatException e) {
                Log.w("FYI", "Failed to parse resource ID in "+session.getUri());
                return error(Response.Status.NOT_FOUND, session);
            }

            StreamableResource resource = null;

            if (getServer().isRegistered(id)) {
                resource = getServer().getResource(id);
            }

            if (resource == null) {
                Log.w("FYI", "Request was not recognized because resource was not found for "+session.getUri());
                return error(Response.Status.NOT_FOUND, session);
            }

            Log.i("FYI", "Validated resource for  "+session.getUri());

            String mimetype = resource.getMimetype();
            long length = resource.getLength();

            int rangeStart = -1;
            int rangeEnd = -1;

            for (String key : session.getHeaders().keySet()) {
                String value = session.getHeaders().get(key);
                Log.i("FYI", "*** HEADER: "+key+": "+value);
            }

            String rangeValue = null;

            if (session.getHeaders().containsKey("Range")) {
                rangeValue = session.getHeaders().get("Range");
            } else if (session.getHeaders().containsKey("range")) {
                rangeValue = session.getHeaders().get("range");
            }

            if (rangeValue != null) {
                Log.i("FYI", "Found range request for "+session.getUri());
                String rangeExpr = rangeValue.trim();
                String[] rangeParts = rangeExpr.split("=");

                if (rangeParts.length == 2) {
                    Log.i("FYI", "Good length for parts in "+session.getUri());
                    String unit = rangeParts[0];
                    String range = rangeParts[1];
                    String[] ends = range.split("-");

                    if (!"bytes".equalsIgnoreCase(unit)) {
                        Log.i("FYI", "Range not satisfiable, unit '"+unit+"' unrecognized, expression: '"+rangeExpr+"' for "+session.getUri());
                        return error(Response.Status.RANGE_NOT_SATISFIABLE, session);
                    }

                    Log.i("FYI", "Ends length "+ends.length);
                    if (ends.length == 1) {
                        String start = ends[0];
                        ends = new String[2];
                        ends[0] = start;
                        ends[1] = "";
                    }

                    if (ends.length == 2) {

                        if ("".equals(ends[1])) {
                            rangeStart = Integer.parseInt(ends[0]);
                            rangeEnd = -1;
                        } else {
                            try {
                                rangeStart = Integer.parseInt(ends[0]);
                                rangeEnd = Integer.parseInt(ends[1]);
                                Log.i("FYI", "Successfully parsed range " + rangeStart + " to " + rangeEnd + ", unit recognized as '" + unit + "' for " + session.getUri());
                            } catch (NumberFormatException e) {
                                Log.e("WTF", "Error parsing HTTP range: " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }

            // Ignore any confusion about range start/end
            if (rangeStart != -1 && rangeEnd != -1 && rangeStart > rangeEnd) {
                Log.w("FYI", "Range invalid with "+rangeStart+" to "+rangeEnd+" for "+session.getUri());
                return error(Response.Status.RANGE_NOT_SATISFIABLE, session);
            }

            if (rangeStart >= 0) {
                Log.w("FYI", "Range request initiated with "+rangeStart+" to "+rangeEnd+" for "+session.getUri());
                final InputStream src = resource.open(rangeStart);

                if (src == null) {
                    Log.e("WTF", "Source is null!");
                }

                int candidateMaxLength = -1;

                if (rangeEnd >= 0)
                    candidateMaxLength = rangeEnd-rangeStart;

                final int maxLength = candidateMaxLength;

                if (rangeEnd >= 0) {
                    if (rangeEnd >= length) {

                        Log.w("FYI", " - File not long enough with for "+rangeStart+" to "+rangeEnd+" for "+session.getUri());
                        return error(Response.Status.RANGE_NOT_SATISFIABLE, session);
                    }

                    InputStream wrapper = new InputStream() {
                        private int bytesRead = 0;

                        @Override
                        public int read() throws IOException {
                            if (bytesRead >= maxLength)
                                return -1;

                            int value = src.read();
                            if (value >= 0)
                                ++bytesRead;                // carefully ordered
                            return value;
                        }

                        @Override
                        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {

                            // Bound the byte count to how much left within the window
                            // This is key to ensure that we do not deliver more than was
                            // requested within the HTTP Range

                            byteCount = Math.min(maxLength - bytesRead, byteCount);

                            int result = src.read(buffer, byteOffset, byteCount);
                            if (result >= 0)
                                bytesRead += result;        // carefully ordered

                            return result;
                        }

                        @Override
                        public void close() throws IOException {
                            src.close();
                            super.close();
                        }
                    };

                    Log.i("FYI", " - Limited range request initiated with "+rangeStart+" to "+rangeEnd+" for "+session.getUri());

                    Response response = new Response(Response.Status.OK, mimetype, wrapper);
                    response.addHeader("Content-Length", (length - rangeStart)+"");

                    return standardize(response);
                } else {
                    Log.w("FYI", " - Unlimited range request initiated with "+rangeStart+" to "+rangeEnd+" for "+session.getUri());
                    // No end range, just give them the stream

                    Response response = new Response(Response.Status.OK, mimetype, src);
                    response.addHeader("Content-Length", (length - rangeStart)+"");

                    return standardize(response);
                }
            } else {
                // Client did not specify Range header.
                Log.w("FYI", "Unlimited request initiated for "+session.getUri());

                // No end range, just give them the stream
                Response response = new Response(Response.Status.OK, mimetype, resource.open(0));
                response.addHeader("Content-Length", length+"");
                return standardize(response);
            }
        }

        public HttpRelayServer getServer() {
            return server;
        }
    }

    private StreamableResource getResource(long id) {
        return streams.get(id);
    }

    /**
     * Check if a resource is registered by ID.
     * @param id
     * @return
     */
    public boolean isRegistered(long id) {
        return streams.containsKey(id);
    }

    protected HTTPD createDaemon()
    {
        Log.i("FYI", "Creating daemon for "+getIpAddress()+":"+getPort());

        return new HTTPD(this, getIpAddress(), getPort());
    }

    private HTTPD httpd;

    public boolean isAlive()
    {
        return httpd.isAlive();
    }

    public void stop()
    {
        httpd.stop();
    }

    public void start() throws IOException
    {
        Log.i("FYI", "//////////// HTTP Relay Server start ////////////////////");
        this.httpd = createDaemon();
        this.httpd.start();
    }
}
