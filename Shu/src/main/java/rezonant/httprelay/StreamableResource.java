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

import java.io.InputStream;

/**
 * Represents a resource which can be streamed using the HttpRelayServer class.
 * Implementations should be able to obtain an InputStream instance representing the
 * stream and also should be able to start it or seek to the requested location provided
 * to the open() call. Additionally, since checks are made against client-requested Ranges
 * and also Content-Length is provided when streaming the entire resource, implementors
 * must provide a getLength(). Finally is is RECOMMENDED that implementors provide a mechanism
 * to retrieve a mimetype for their resource synchronously. This mimetype will be provided to
 * the client as the Content-Type header. If mimetypes are not important or not yet implemented
 * for the implementation, getMimetype() should return "application/x-unknown" to conform to
 * existing norms.
 *
 * Created by liam on 7/28/14.
 */
public interface StreamableResource {
    /**
     * Return an InputStream which represents the resource which starts at the given offset.
     * The stream will not be seeked against, so it is acceptable to open a full InputStream
     * of the resource and then simply seek to the desired position before returning.
     *
     * @param offset The offset to start at or seek to within the resource
     * @return A stream which can be used to read the rest of the data in the file. Note that
     *          in most circumstances the whole file will NOT be read. The InputStream implementation
     *          should be written efficiently for these cases.
     */
    InputStream open(long offset);

    /**
     * Return a mimetype for this resource, if available. If not available, return "application/x-unknown"
     * to confirm to existing norms. This value is used to popuylate the Content-Type header.
     *
     * @return
     */
    String getMimetype();

    /**
     * Get the total length in bytes of this resource. Must be accurate to allow resource to be
     * streamed properly. This method is called before every request to determine if desired range
     * is satisfiable and/or to populate the Content-Length header.
     * @return
     */
    long getLength();
}
