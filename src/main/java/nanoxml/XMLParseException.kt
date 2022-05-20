/* XMLParseException.java
 *
 * $Revision: 1.4 $
 * $Date: 2002/03/24 10:27:59 $
 * $Name: RELEASE_2_2_1 $
 *
 * This file is part of NanoXML 2 Lite.
 * Copyright (C) 2000-2002 Marc De Scheemaecker, All Rights Reserved.
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software in
 *     a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source distribution.
 *****************************************************************************/
package nanoxml

/**
 * An XMLParseException is thrown when an error occures while parsing an XML
 * string.
 * <P>
 * $Revision: 1.4 $<BR></BR>
 * $Date: 2002/03/24 10:27:59 $</P><P>
 *
 * @see nanoxml.XMLElement
 *
 *
 * @author Marc De Scheemaecker
 * @version $Name: RELEASE_2_2_1 $, $Revision: 1.4 $
</P> */
class XMLParseException : RuntimeException {
    /**
     * Where the error occurred, or `NO_LINE` if the line number is
     * unknown.
     *
     * @see nanoxml.XMLParseException.NO_LINE
     */
    /**
     * The line number in the source code where the error occurred, or
     * `NO_LINE` if the line number is unknown.
     *
     * <dl><dt>**Invariants:**</dt><dd>
     *  * `lineNr &gt 0 || lineNr == NO_LINE`
    </dd></dl> *
     */
    var lineNr: Int
        private set

    /**
     * Creates an exception.
     *
     * @param name    The name of the element where the error is located.
     * @param message A message describing what went wrong.
     *
     * <dl><dt>**Preconditions:**</dt><dd>
     *  * `message != null`
    </dd></dl> *
     *
     * <dl><dt>**Postconditions:**</dt><dd>
     *  * getLineNr() => NO_LINE
    </dd></dl> * <dl>
    </dl> */
    constructor(
        name: String?,
        message: String
    ) : super(
        "XML Parse Exception during parsing of "
                + (if (name == null) "the XML definition" else "a $name element")
                + ": " + message
    ) {
        lineNr = NO_LINE
    }

    /**
     * Creates an exception.
     *
     * @param name    The name of the element where the error is located.
     * @param lineNr  The number of the line in the input.
     * @param message A message describing what went wrong.
     *
     * <dl><dt>**Preconditions:**</dt><dd>
     *  * `message != null`
     *  * `lineNr > 0`
    </dd></dl> *
     *
     * <dl><dt>**Postconditions:**</dt><dd>
     *  * getLineNr() => lineNr
    </dd></dl> * <dl>
    </dl> */
    constructor(
        name: String?,
        lineNr: Int,
        message: String
    ) : super(
        "XML Parse Exception during parsing of "
                + (if (name == null) "the XML definition" else "a $name element")
                + " at line " + lineNr + ": " + message
    ) {
        this.lineNr = lineNr
    }

    companion object {
        /**
         * Indicates that no line number has been associated with this exception.
         */
        const val NO_LINE = -1
    }
}