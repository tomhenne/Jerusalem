/* XMLElement.java
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

import nanoxml.XMLElement
import java.io.*
import java.util.*

/**
 * XMLElement is a representation of an XML object. The object is able to parse
 * XML code.
 * <P>
</P> * <DL>
 * <DT><B>Parsing XML Data</B></DT>
 * <DD>
 * You can parse XML data using the following code:
 * <UL>
 * <CODE>
 * XMLElement xml = new XMLElement();<BR></BR>
 * FileReader reader = new FileReader("filename.xml");<BR></BR>
 * xml.parseFromReader(reader);
</CODE> *
</UL> *
</DD> *
</DL> *
 * <DL>
 * <DT><B>Retrieving Attributes</B></DT>
 * <DD>
 * You can enumerate the attributes of an element using the method
 * [enumerateAttributeNames][.enumerateAttributeNames]. The attribute
 * values can be retrieved using the method
 * [getStringAttribute][.getStringAttribute]. The
 * following example shows how to list the attributes of an element:
 * <UL>
 * <CODE>
 * XMLElement element = ...;<BR></BR>
 * Enumeration enum = element.getAttributeNames();<BR></BR>
 * while (enum.hasMoreElements()) {<BR></BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;String key = (String) enum.nextElement();<BR></BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;String value = element.getStringAttribute(key);<BR></BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;System.out.println(key + " = " + value);<BR></BR>
 * }
</CODE> *
</UL> *
</DD> *
</DL> *
 * <DL>
 * <DT><B>Retrieving Child Elements</B></DT>
 * <DD>
 * You can enumerate the children of an element using
 * [enumerateChildren][.enumerateChildren]. The number of child elements
 * can be retrieved using [countChildren][.countChildren].</DD>
</DL> *
 * <DL>
 * <DT><B>Elements Containing Character Data</B></DT>
 * <DD>
 * If an elements contains character data, like in the following example:
 * <UL>
 * <CODE>
 * &lt;title&gt;The Title&lt;/title&gt;
</CODE> *
</UL> *
 * you can retrieve that data using the method [getContent][.getContent].
</DD> *
</DL> *
 * <DL>
 * <DT><B>Subclassing XMLElement</B></DT>
 * <DD>
 * When subclassing XMLElement, you need to override the method
 * [createAnotherElement][.createAnotherElement] which has to return a
 * new copy of the receiver.</DD>
</DL> *
 * <P>
 *
 * @see nanoxml.XMLParseException
 *
 *
 * @author Marc De Scheemaecker &lt;<A href="mailto:cyberelf@mac.com">cyberelf@mac.com</A>&gt;
 * @version $Name: RELEASE_2_2_1 $, $Revision: 1.4 $
</P> */
class XMLElement protected constructor(
    /**
     * `true` if the leading and trailing whitespace of #PCDATA
     * sections have to be ignored.
     */
    private val ignoreWhitespace: Boolean,
    /**
     * `true` if the case of the element and attribute names are case
     * insensitive.
     */
    private val ignoreCase: Boolean
) {
    /**
     * The attributes given to the element.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * The field can be empty.
     *  * The field is never `null`.
     *  * The keys and the values are strings.
     *
    </dd> *
    </dl> *
     */
    private var attributes: Hashtable<String, String>

    /**
     * Child elements of the element.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * The field can be empty.
     *  * The field is never `null`.
     *  * The elements are instances of `XMLElement` or a subclass
     * of `XMLElement`.
     *
    </dd> *
    </dl> *
     */
    private var children: LinkedList<XMLElement>
    /**
     * Returns the name of the element.
     *
     * @see nanoxml.XMLElement.setName
     */
    /**
     * Changes the name of the element.
     *
     * @param name
     * The new name.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     *
     * @see nanoxml.XMLElement.getName
     */
    /**
     * The name of the element.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * The field is `null` iff the element is not initialized by
     * either parse or setName.
     *  * If the field is not `null`, it's not empty.
     *  * If the field is not `null`, it contains a valid XML
     * identifier.
     *
    </dd> *
    </dl> *
     */
    var name: String? = null
    /**
     * Returns the PCDATA content of the object. If there is no such content,
     * <CODE>null</CODE> is returned.
     *
     * @see nanoxml.XMLElement.setContent
     */
    /**
     * Changes the content string.
     *
     * @param content
     * The new content string.
     */
    /**
     * The #PCDATA content of the object.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * The field is `null` iff the element is not a #PCDATA
     * element.
     *  * The field can be any string, including the empty string.
     *
    </dd> *
    </dl> *
     */
    var content: String? = ""
    /**
     * Returns the line nr in the source data on which the element is found.
     * This method returns `0` there is no associated source data.
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * `result >= 0`
     *
    </dd> *
    </dl> *
     */
    /**
     * The line number where the element starts.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * `lineNr &gt= 0`
     *
    </dd> *
    </dl> *
     */
    val lineNr: Int

    /**
     * Character read too much. This character provides push-back functionality
     * to the input reader without having to use a PushbackReader. If there is
     * no such character, this field is '\0'.
     */
    private var charReadTooMuch = 0.toChar()

    /**
     * The reader provided by the caller of the parse method.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * The field is not `null` while the parse method is running.
     *
    </dd> *
    </dl> *
     */
    private var reader: Reader? = null

    /**
     * The current line number in the source content.
     *
     * <dl>
     * <dt>**Invariants:**</dt>
     * <dd>
     *
     *  * parserLineNr &gt; 0 while the parse method is running.
     *
    </dd> *
    </dl> *
     */
    private var parserLineNr = 0

    /**
     * Creates and initializes a new XML element. Calling the construction is
     * equivalent to:
     *
     * `new XMLElement(new Hashtable(), false, true)
    ` *
     *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * countChildren() => 0
     *  * enumerateChildren() => empty enumeration
     *  * enumeratePropertyNames() => empty enumeration
     *  * getChildren() => empty LinkedList
     *  * getContent() => ""
     *  * getLineNr() => 0
     *  * getName() => null
     *
    </dd> *
    </dl> *
     *
     * @see nanoxml.XMLElement.XMLElement
     * @see nanoxml.XMLElement.XMLElement
     * @see nanoxml.XMLElement.XMLElement
     */
    constructor() : this(false, false) {}

    /**
     * Creates and initializes a new XML element. Calling the construction is
     * equivalent to:
     *
     * `new XMLElement(new Hashtable(), skipLeadingWhitespace, true)
    ` *
     *
     *
     * @param skipLeadingWhitespace
     * `true` if leading and trailing whitespace in PCDATA
     * content has to be removed.
     *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * countChildren() => 0
     *  * enumerateChildren() => empty enumeration
     *  * enumeratePropertyNames() => empty enumeration
     *  * getChildren() => empty LinkedList
     *  * getContent() => ""
     *  * getLineNr() => 0
     *  * getName() => null
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.XMLElement
     * @see nanoxml.XMLElement.XMLElement
     * @see nanoxml.XMLElement.XMLElement
    </dl> */
    constructor(skipLeadingWhitespace: Boolean) : this(skipLeadingWhitespace, true) {}

    /**
     * Creates and initializes a new XML element.
     * <P>
     * This constructor should <I>only</I> be called from
     * [createAnotherElement][.createAnotherElement] to create child
     * elements.
     *
     * @param entities
     * The entity conversion table.
     * @param skipLeadingWhitespace
     * `true` if leading and trailing whitespace in PCDATA
     * content has to be removed.
     * @param fillBasicConversionTable
     * `true` if the basic entities need to be added to
     * the entity list.
     * @param ignoreCase
     * `true` if the case of element and attribute names
     * have to be ignored.
     *
     *
    </P> * <dl>
     * <dt>**Preconditions:**</dt><dd>
     *
     *  * `entities != null`  * if `
     * fillBasicConversionTable == false` then `entities
    ` *  contains at least the following entries: `amp
    ` * , `lt`, `gt`, `apos`
     * and `quot`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt><dd>
     *
     *  * countChildren() => 0  * enumerateChildren() => empty
     * enumeration  * enumeratePropertyNames() => empty enumeration
     *  * getChildren() => empty LinkedList  * getContent() => ""  *
     * getLineNr() => 0  * getName() => null
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.createAnotherElement
    </dl> */
    init {
        attributes = Hashtable()
        children = LinkedList()
        lineNr = 0
        if (defaultEntities.size == 0) {
            defaultEntities["amp"] = charArrayOf('&')
            defaultEntities["quot"] = charArrayOf('"')
            defaultEntities["apos"] = charArrayOf('\'')
            defaultEntities["lt"] = charArrayOf('<')
            defaultEntities["gt"] = charArrayOf('>')
        }
    }

    /**
     * Adds a child element.
     *
     * @param child
     * The child element to add.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `child != null`
     *  * `child.getName() != null`
     *  * `child` does not have a parent element
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * countChildren() => old.countChildren() + 1
     *  * enumerateChildren() => old.enumerateChildren() + child
     *  * getChildren() => old.enumerateChildren() + child
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.countChildren
     * @see nanoxml.XMLElement.enumerateChildren
     * @see nanoxml.XMLElement.getChildren
     * @see nanoxml.XMLElement.removeChild
    </dl> */
    fun addChild(child: XMLElement) {
        children.add(child)
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name
     * The name of the attribute.
     * @param value
     * The value of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *  * `value != null`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * enumerateAttributeNames() => old.enumerateAttributeNames()
     * + name
     *  * getAttribute(name) => value
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
    </dl> */
    fun setAttribute(name: String, value: Any) {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        attributes[name] = value.toString()
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name
     * The name of the attribute.
     * @param value
     * The value of the attribute.
     *
     */
    @Deprecated(
        """Use {@link #setAttribute(java.lang.String, java.lang.Object)
	 *             setAttribute} instead."""
    )
    fun addProperty(name: String, value: Any) {
        setAttribute(name, value)
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name
     * The name of the attribute.
     * @param value
     * The value of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * enumerateAttributeNames() => old.enumerateAttributeNames()
     * + name
     *  * getIntAttribute(name) => value
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
    </dl> */
    fun setIntAttribute(name: String, value: Int) {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        attributes[name] = Integer.toString(value)
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name
     * The name of the attribute.
     * @param value
     * The value of the attribute.
     *
     */
    @Deprecated(
        """Use {@link #setIntAttribute(java.lang.String, int)
	 *             setIntAttribute} instead."""
    )
    fun addProperty(key: String, value: Int) {
        setIntAttribute(key, value)
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name
     * The name of the attribute.
     * @param value
     * The value of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * enumerateAttributeNames() => old.enumerateAttributeNames()
     * + name
     *  * getDoubleAttribute(name) => value
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
    </dl> */
    fun setDoubleAttribute(name: String, value: Double) {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        attributes[name] = java.lang.Double.toString(value)
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name
     * The name of the attribute.
     * @param value
     * The value of the attribute.
     *
     */
    @Deprecated(
        """Use {@link #setDoubleAttribute(java.lang.String, double)
	 *             setDoubleAttribute} instead."""
    )
    fun addProperty(name: String, value: Double) {
        setDoubleAttribute(name, value)
    }

    /**
     * Returns the number of child elements of the element.
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * `result >= 0`
     *
    </dd> *
    </dl> *
     *
     * @see nanoxml.XMLElement.addChild
     * @see nanoxml.XMLElement.enumerateChildren
     * @see nanoxml.XMLElement.getChildren
     * @see nanoxml.XMLElement.removeChild
     */
    fun countChildren(): Int {
        return children.size
    }

    /**
     * Enumerates the attribute names.
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * `result != null`
     *
    </dd> *
    </dl> *
     *
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getBooleanAttribute
     */
    fun enumerateAttributeNames(): Enumeration<String> {
        return attributes.keys()
    }

    /**
     * Enumerates the child elements.
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * `result != null`
     *
    </dd> *
    </dl> *
     *
     * @see nanoxml.XMLElement.addChild
     * @see nanoxml.XMLElement.countChildren
     * @see nanoxml.XMLElement.getChildren
     * @see nanoxml.XMLElement.removeChild
     */
    val childrenIterator: Iterator<XMLElement>
        get() = children.iterator()

    /**
     * Returns the child elements as a LinkedList. It is safe to modify this LinkedList.
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * `result != null`
     *
    </dd> *
    </dl> *
     *
     * @see nanoxml.XMLElement.addChild
     * @see nanoxml.XMLElement.countChildren
     * @see nanoxml.XMLElement.enumerateChildren
     * @see nanoxml.XMLElement.removeChild
     */
    fun getChildren(): LinkedList<XMLElement>? {
        return try {
            children.clone() as LinkedList<XMLElement>
        } catch (e: Exception) {
            // this never happens, however, some Java compilers are so
            // braindead that they require this exception clause
            null
        }
    }

    val childMap: Map<String?, XMLElement>
        get() {
            val params: MutableMap<String?, XMLElement> = HashMap()
            for (el in getChildren()!!) params[el.name] = el
            return params
        }

    fun getChild(name: String): XMLElement? {
        for (i in children.indices) {
            val n = children[i]
            if (name == n.name) return n
        }
        return null
    }

    fun getChildByPath(path: String): XMLElement? {
        var path = path
        while (path.startsWith("/")) path = path.substring(1)
        if (path.length == 0) return this
        val p = path.indexOf('/')
        if (p < 0) return getChild(path)
        val head = path.substring(0, p)
        val tail = path.substring(p + 1)
        val n = getChild(head)
        return n?.getChildByPath(tail)
    }

    /**
     * Returns the PCDATA content of the object. If there is no such content,
     * <CODE>null</CODE> is returned.
     *
     */
    @Deprecated("Use {@link #getContent() getContent} instead.")
    fun getContents(): String? {
        return content
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `null` is returned.
     *
     * @param name
     * The name of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
    </dl> */
    fun getAttribute(name: String): Any? {
        return this.getAttribute(name, null)
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `defaultValue` is returned.
     *
     * @param name
     * The name of the attribute.
     * @param defaultValue
     * Key to use if the attribute is missing.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
    </dl> */
    fun getAttribute(name: String, defaultValue: Any?): Any? {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        var value: Any? = attributes[name]
        if (value == null) {
            value = defaultValue
        }
        return value
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * <P>
     * As an example, if valueSet contains the mapping `"one" =>
     * "1"` and the element contains the attribute `attr="one"`
     * , then `getAttribute("attr", mapping, defaultKey, false)`
     * returns `"1"`.
     *
     * @param name
     * The name of the attribute.
     * @param valueSet
     * Hashtable mapping keys to values.
     * @param defaultKey
     * Key to use if the attribute is missing.
     * @param allowLiterals
     * `true` if literals are valid.
     *
     *
    </P> * <dl>
     * <dt>**Preconditions:**</dt><dd>
     *
     *  * `name != null`  * `name` is a valid
     * XML identifier  * `valueSet` != null  * the keys
     * of `valueSet` are strings
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
    </dl> */
    fun getAttribute(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?, allowLiterals: Boolean
    ): Any? {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        var key: Any? = attributes[name]
        var result: Any?
        if (key == null) {
            key = defaultKey
        }
        result = valueSet[key]
        if (result == null) {
            result = if (allowLiterals) {
                key
            } else {
                throw invalidValue(name, key as String?)
            }
        }
        return result
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `null` is returned.
     *
     * @param name
     * The name of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
    </dl> */
    fun getStringAttribute(name: String): String? {
        return this.getStringAttribute(name, null)
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `defaultValue` is returned.
     *
     * @param name
     * The name of the attribute.
     * @param defaultValue
     * Key to use if the attribute is missing.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
    </dl> */
    fun getStringAttribute(name: String, defaultValue: String?): String? {
        return this.getAttribute(name, defaultValue) as String?
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * <P>
     * As an example, if valueSet contains the mapping `"one" =>
     * "1"` and the element contains the attribute `attr="one"`
     * , then `getAttribute("attr", mapping, defaultKey, false)`
     * returns `"1"`.
     *
     * @param name
     * The name of the attribute.
     * @param valueSet
     * Hashtable mapping keys to values.
     * @param defaultKey
     * Key to use if the attribute is missing.
     * @param allowLiterals
     * `true` if literals are valid.
     *
     *
    </P> * <dl>
     * <dt>**Preconditions:**</dt><dd>
     *
     *  * `name != null`  * `name` is a valid
     * XML identifier  * `valueSet` != null  * the keys
     * of `valueSet` are strings  * the values of `
     * valueSet` are strings
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
    </dl> */
    fun getStringAttribute(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?, allowLiterals: Boolean
    ): String? {
        return this.getAttribute(
            name, valueSet, defaultKey,
            allowLiterals
        ) as String?
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `0` is returned.
     *
     * @param name
     * The name of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
    </dl> */
    fun getIntAttribute(name: String): Int {
        return this.getIntAttribute(name, 0)
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `defaultValue` is returned.
     *
     * @param name
     * The name of the attribute.
     * @param defaultValue
     * Key to use if the attribute is missing.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
    </dl> */
    fun getIntAttribute(name: String, defaultValue: Int): Int {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        val value = attributes[name]
        return if (value == null) {
            defaultValue
        } else {
            try {
                value.toInt()
            } catch (e: NumberFormatException) {
                throw invalidValue(name, value)
            }
        }
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * <P>
     * As an example, if valueSet contains the mapping `"one" => 1`
     * and the element contains the attribute `attr="one"`, then
     * `getIntAttribute("attr", mapping, defaultKey, false)` returns
     * `1`.
     *
     * @param name
     * The name of the attribute.
     * @param valueSet
     * Hashtable mapping keys to values.
     * @param defaultKey
     * Key to use if the attribute is missing.
     * @param allowLiteralNumbers
     * `true` if literal numbers are valid.
     *
     *
    </P> * <dl>
     * <dt>**Preconditions:**</dt><dd>
     *
     *  * `name != null`  * `name` is a valid
     * XML identifier  * `valueSet` != null  * the keys
     * of `valueSet` are strings  * the values of `
     * valueSet` are Integer objects  * `defaultKey
    ` *  is either `null`, a key in `valueSet
    ` *  or an integer.
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
    </dl> */
    fun getIntAttribute(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?, allowLiteralNumbers: Boolean
    ): Int {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        var key: Any? = attributes[name]
        var result: Int?
        if (key == null) {
            key = defaultKey
        }
        result = try {
            valueSet[key] as Int?
        } catch (e: ClassCastException) {
            throw invalidValueSet(name)
        }
        if (result == null) {
            if (!allowLiteralNumbers) {
                throw invalidValue(name, key as String?)
            }
            result = try {
                Integer.valueOf(key as String?)
            } catch (e: NumberFormatException) {
                throw invalidValue(name, key as String?)
            }
        }
        return result!!.toInt()
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `0.0` is returned.
     *
     * @param name
     * The name of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
    </dl> */
    fun getDoubleAttribute(name: String): Double {
        return this.getDoubleAttribute(name, 0.0)
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `defaultValue` is returned.
     *
     * @param name
     * The name of the attribute.
     * @param defaultValue
     * Key to use if the attribute is missing.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
    </dl> */
    fun getDoubleAttribute(name: String, defaultValue: Double): Double {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        val value = attributes[name]
        return if (value == null) {
            defaultValue
        } else {
            try {
                java.lang.Double.valueOf(value).toDouble()
            } catch (e: NumberFormatException) {
                throw invalidValue(name, value)
            }
        }
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * <P>
     * As an example, if valueSet contains the mapping `"one" =>
     * 1.0` and the element contains the attribute `attr="one"`
     * , then
     * `getDoubleAttribute("attr", mapping, defaultKey, false)`
     * returns `1.0`.
     *
     * @param name
     * The name of the attribute.
     * @param valueSet
     * Hashtable mapping keys to values.
     * @param defaultKey
     * Key to use if the attribute is missing.
     * @param allowLiteralNumbers
     * `true` if literal numbers are valid.
     *
     *
    </P> * <dl>
     * <dt>**Preconditions:**</dt><dd>
     *
     *  * `name != null`  * `name` is a valid
     * XML identifier  * `valueSet != null`  * the keys
     * of `valueSet` are strings  * the values of `
     * valueSet` are Double objects  * `defaultKey`
     * is either `null`, a key in `valueSet` or
     * a double.
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
    </dl> */
    fun getDoubleAttribute(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?, allowLiteralNumbers: Boolean
    ): Double {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        var key: Any? = attributes[name]
        var result: Double?
        if (key == null) {
            key = defaultKey
        }
        result = try {
            valueSet[key] as Double?
        } catch (e: ClassCastException) {
            throw invalidValueSet(name)
        }
        if (result == null) {
            if (!allowLiteralNumbers) {
                throw invalidValue(name, key as String?)
            }
            result = try {
                java.lang.Double.valueOf(key as String?)
            } catch (e: NumberFormatException) {
                throw invalidValue(name, key as String?)
            }
        }
        return result!!.toDouble()
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist,
     * `defaultValue` is returned. If the value of the attribute is
     * equal to `trueValue`, `true` is returned. If the
     * value of the attribute is equal to `falseValue`,
     * `false` is returned. If the value doesn't match
     * `trueValue` or `falseValue`, an exception is
     * thrown.
     *
     * @param name
     * The name of the attribute.
     * @param trueValue
     * The value associated with `true`.
     * @param falseValue
     * The value associated with `true`.
     * @param defaultValue
     * Value to use if the attribute is missing.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *  * `trueValue` and `falseValue` are
     * different strings.
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.removeAttribute
     * @see nanoxml.XMLElement.enumerateAttributeNames
    </dl> */
    fun getBooleanAttribute(
        name: String, trueValue: String,
        falseValue: String, defaultValue: Boolean
    ): Boolean {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        val value: Any? = attributes[name]
        return if (value == null) {
            defaultValue
        } else if (value == trueValue) {
            true
        } else if (value == falseValue) {
            false
        } else {
            throw invalidValue(name, value as String?)
        }
    }

    /**
     * Returns an attribute by looking up a key in a hashtable.
     *
     */
    @Deprecated(
        """Use
	              {@link #getIntAttribute(java.lang.String, java.util.Hashtable, java.lang.String, boolean)
	 *             getIntAttribute} instead."""
    )
    fun getIntProperty(name: String, valueSet: Hashtable<*, *>, defaultKey: String?): Int {
        return this.getIntAttribute(name, valueSet, defaultKey, false)
    }

    /**
     * Returns an attribute.
     *
     */
    @Deprecated(
        """Use {@link #getStringAttribute(java.lang.String)
	 *             getStringAttribute} instead."""
    )
    fun getProperty(name: String): String? {
        return this.getStringAttribute(name)
    }

    /**
     * Returns an attribute.
     *
     */
    @Deprecated(
        """Use
	              {@link #getStringAttribute(java.lang.String, java.lang.String)
	 *             getStringAttribute} instead."""
    )
    fun getProperty(name: String, defaultValue: String?): String? {
        return this.getStringAttribute(name, defaultValue)
    }

    /**
     * Returns an attribute.
     *
     */
    @Deprecated(
        """Use {@link #getIntAttribute(java.lang.String, int)
	 *             getIntAttribute} instead."""
    )
    fun getProperty(name: String, defaultValue: Int): Int {
        return this.getIntAttribute(name, defaultValue)
    }

    /**
     * Returns an attribute.
     *
     */
    @Deprecated(
        """Use {@link #getDoubleAttribute(java.lang.String, double)
	 *             getDoubleAttribute} instead."""
    )
    fun getProperty(name: String, defaultValue: Double): Double {
        return this.getDoubleAttribute(name, defaultValue)
    }

    /**
     * Returns an attribute.
     *
     */
    @Deprecated(
        """Use
	              {@link #getBooleanAttribute(java.lang.String, java.lang.String, java.lang.String, boolean)
	 *             getBooleanAttribute} instead."""
    )
    fun getProperty(
        key: String, trueValue: String, falseValue: String,
        defaultValue: Boolean
    ): Boolean {
        return getBooleanAttribute(
            key, trueValue, falseValue,
            defaultValue
        )
    }

    /**
     * Returns an attribute by looking up a key in a hashtable.
     *
     */
    @Deprecated(
        """Use
	              {@link #getAttribute(java.lang.String, java.util.Hashtable, java.lang.String, boolean)
	 *             getAttribute} instead."""
    )
    fun getProperty(name: String, valueSet: Hashtable<*, *>, defaultKey: String?): Any? {
        return this.getAttribute(name, valueSet, defaultKey, false)
    }

    /**
     * Returns an attribute by looking up a key in a hashtable.
     *
     */
    @Deprecated(
        """Use
	              {@link #getStringAttribute(java.lang.String, java.util.Hashtable, java.lang.String, boolean)
	 *             getStringAttribute} instead."""
    )
    fun getStringProperty(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?
    ): String? {
        return this.getStringAttribute(name, valueSet, defaultKey, false)
    }

    /**
     * Returns an attribute by looking up a key in a hashtable.
     *
     */
    @Deprecated(
        """Use
	              {@link #getIntAttribute(java.lang.String, java.util.Hashtable, java.lang.String, boolean)
	 *             getIntAttribute} instead."""
    )
    fun getSpecialIntProperty(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?
    ): Int {
        return this.getIntAttribute(name, valueSet, defaultKey, true)
    }

    /**
     * Returns an attribute by looking up a key in a hashtable.
     *
     */
    @Deprecated(
        """Use
	              {@link #getDoubleAttribute(java.lang.String, java.util.Hashtable, java.lang.String, boolean)
	 *             getDoubleAttribute} instead."""
    )
    fun getSpecialDoubleProperty(
        name: String, valueSet: Hashtable<*, *>,
        defaultKey: String?
    ): Double {
        return this.getDoubleAttribute(name, valueSet, defaultKey, true)
    }

    /**
     * Returns the name of the element.
     *
     */
    @get:Deprecated("Use {@link #getName() getName} instead.")
    val tagName: String?
        get() = name
    /**
     * Reads one XML element from a java.io.Reader and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     * @param startingLineNr
     * The line number of the first line in the data.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `reader != null`
     *  * `reader` is not closed
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *  * the reader points to the first character following the
     * last '&gt;' character of the XML element
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws java.io.IOException
     * If an error occured while reading the input.
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the read data.
    </dl> */
    /**
     * Reads one XML element from a java.io.Reader and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `reader != null`
     *  * `reader` is not closed
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *  * the reader points to the first character following the
     * last '&gt;' character of the XML element
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws java.io.IOException
     * If an error occured while reading the input.
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the read data.
    </dl> */
    @JvmOverloads
    @Throws(IOException::class, XMLParseException::class)
    fun parseFromReader(reader: Reader?, startingLineNr: Int =  /* startingLineNr */1) {
        name = null
        content = ""
        attributes = Hashtable()
        children = LinkedList()
        charReadTooMuch = '\u0000'
        this.reader = reader
        parserLineNr = startingLineNr

        // added by Tom
        // skip initial content to avoid conflicts with UTF file headers
        while (true) {
            val ch = readChar()
            if (ch == '<') {
                unreadChar(ch)
                break
            }
        }
        while (true) {
            var ch = this.scanWhitespace()
            if (ch != '<') {
                throw expectedInput("<")
            }
            ch = readChar()
            if (ch == '!' || ch == '?') {
                skipSpecialTag(0)
            } else {
                unreadChar(ch)
                scanElement(this)
                return
            }
        }
    }

    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `string != null`
     *  * `string.length() > 0`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the string.
    </dl> */
    @Throws(XMLParseException::class)
    fun parseString(string: String?) {
        try {
            parseFromReader(
                StringReader(string),  /* startingLineNr */
                1
            )
        } catch (e: IOException) {
            // Java exception handling suxx
        }
    }

    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     * @param offset
     * The first character in `string` to scan.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `string != null`
     *  * `offset < string.length()`
     *  * `offset >= 0`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the string.
    </dl> */
    @Throws(XMLParseException::class)
    fun parseString(string: String, offset: Int) {
        this.parseString(string.substring(offset))
    }

    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     * @param offset
     * The first character in `string` to scan.
     * @param end
     * The character where to stop scanning. This character is not
     * scanned.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `string != null`
     *  * `end <= string.length()`
     *  * `offset < end`
     *  * `offset >= 0`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the string.
    </dl> */
    @Throws(XMLParseException::class)
    fun parseString(string: String, offset: Int, end: Int) {
        this.parseString(string.substring(offset, end))
    }

    /**
     * Reads one XML element from a String and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     * @param offset
     * The first character in `string` to scan.
     * @param end
     * The character where to stop scanning. This character is not
     * scanned.
     * @param startingLineNr
     * The line number of the first line in the data.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `string != null`
     *  * `end <= string.length()`
     *  * `offset < end`
     *  * `offset >= 0`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the string.
    </dl> */
    @Throws(XMLParseException::class)
    fun parseString(
        string: String, offset: Int, end: Int,
        startingLineNr: Int
    ) {
        var string = string
        string = string.substring(offset, end)
        try {
            parseFromReader(StringReader(string), startingLineNr)
        } catch (e: IOException) {
            // Java exception handling suxx
        }
    }
    /**
     * Reads one XML element from a char array and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     * @param offset
     * The first character in `string` to scan.
     * @param end
     * The character where to stop scanning. This character is not
     * scanned.
     * @param startingLineNr
     * The line number of the first line in the data.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `input != null`
     *  * `end <= input.length`
     *  * `offset < end`
     *  * `offset >= 0`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the string.
    </dl> */
    /**
     * Reads one XML element from a char array and parses it.
     *
     * @param reader
     * The reader from which to retrieve the XML data.
     * @param offset
     * The first character in `string` to scan.
     * @param end
     * The character where to stop scanning. This character is not
     * scanned.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `input != null`
     *  * `end <= input.length`
     *  * `offset < end`
     *  * `offset >= 0`
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * the state of the receiver is updated to reflect the XML
     * element parsed from the reader
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @throws nanoxml.XMLParseException
     * If an error occured while parsing the string.
    </dl> */
    @JvmOverloads
    @Throws(XMLParseException::class)
    fun parseCharArray(
        input: CharArray?, offset: Int, end: Int,
        startingLineNr: Int =  /* startingLineNr */1
    ) {
        try {
            val reader: Reader = CharArrayReader(input, offset, end)
            parseFromReader(reader, startingLineNr)
        } catch (e: IOException) {
            // This exception will never happen.
        }
    }

    /**
     * Removes a child element.
     *
     * @param child
     * The child element to remove.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `child != null`
     *  * `child` is a child element of the receiver
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * countChildren() => old.countChildren() - 1
     *  * enumerateChildren() => old.enumerateChildren() - child
     *  * getChildren() => old.enumerateChildren() - child
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.addChild
     * @see nanoxml.XMLElement.countChildren
     * @see nanoxml.XMLElement.enumerateChildren
     * @see nanoxml.XMLElement.getChildren
    </dl> */
    fun removeChild(child: XMLElement) {
        children.remove(child)
    }

    /**
     * Removes an attribute.
     *
     * @param name
     * The name of the attribute.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name` is a valid XML identifier
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * enumerateAttributeNames() => old.enumerateAttributeNames()
     * - name
     *  * getAttribute(name) => `null`
     *
    </dd> *
    </dl> *
     * <dl>
     *
     * @see nanoxml.XMLElement.enumerateAttributeNames
     * @see nanoxml.XMLElement.setDoubleAttribute
     * @see nanoxml.XMLElement.setIntAttribute
     * @see nanoxml.XMLElement.setAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getStringAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getIntAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getDoubleAttribute
     * @see nanoxml.XMLElement.getBooleanAttribute
    </dl> */
    fun removeAttribute(name: String) {
        var name = name
        if (ignoreCase) {
            name = name.uppercase(Locale.getDefault())
        }
        attributes.remove(name)
    }

    /**
     * Removes an attribute.
     *
     * @param name
     * The name of the attribute.
     *
     */
    @Deprecated(
        """Use {@link #removeAttribute(java.lang.String)
	 *             removeAttribute} instead."""
    )
    fun removeProperty(name: String) {
        removeAttribute(name)
    }

    /**
     * Removes an attribute.
     *
     * @param name
     * The name of the attribute.
     *
     */
    @Deprecated(
        """Use {@link #removeAttribute(java.lang.String)
	 *             removeAttribute} instead."""
    )
    fun removeChild(name: String) {
        removeAttribute(name)
    }

    /**
     * Creates a new similar XML element.
     * <P>
     * You should override this method when subclassing XMLElement.
    </P> */
    protected fun createAnotherElement(): XMLElement {
        return XMLElement(
            ignoreWhitespace,
            ignoreCase
        )
    }

    /**
     * Writes the XML element to a string.
     *
     * @see nanoxml.XMLElement.write
     */
    override fun toString(): String {
        return try {
            val out = ByteArrayOutputStream()
            val writer = OutputStreamWriter(out)
            write(writer)
            writer.flush()
            String(out.toByteArray())
        } catch (e: IOException) {
            // Java exception handling suxx
            super.toString()
        }
    }

    /**
     * Writes the XML element to a writer.
     *
     * @param writer
     * The writer to write the XML data to.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `writer != null`
     *  * `writer` is not closed
     *
    </dd> *
    </dl> *
     *
     * @throws java.io.IOException
     * If the data could not be written to the writer.
     *
     * @see nanoxml.XMLElement.toString
     */
    @Throws(IOException::class)
    fun write(writer: Writer) {
        if (name == null) {
            writeEncoded(writer, content)
            return
        }
        writer.write('<'.code)
        writer.write(name)
        if (!attributes.isEmpty) {
            val enume = attributes.keys()
            while (enume.hasMoreElements()) {
                writer.write(' '.code)
                val key = enume.nextElement() as String
                writer.write(key)
                writer.write('='.code)
                writer.write('"'.code)
                writeEncoded(writer, attributes[key])
                writer.write('"'.code)
            }
        }
        if (content != null && content!!.length > 0) {
            writer.write('>'.code)
            writeEncoded(writer, content)
            writer.write('<'.code)
            writer.write('/'.code)
            writer.write(name)
            writer.write('>'.code)
        } else if (children.isEmpty()) {
            writer.write('/'.code)
            writer.write('>'.code)
        } else {
            writer.write('>'.code)
            val enume = childrenIterator
            while (enume.hasNext()) {
                enume.next().write(writer)
            }
            writer.write('<'.code)
            writer.write('/'.code)
            writer.write(name)
            writer.write('>'.code)
        }
    }

    /**
     * Writes a string encoded to a writer.
     *
     * @param writer
     * The writer to write the XML data to.
     * @param str
     * The string to write encoded.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `writer != null`
     *  * `writer` is not closed
     *  * `str != null`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun writeEncoded(writer: Writer, str: String?) {
        var i = 0
        while (i < str!!.length) {
            val ch = str[i]
            when (ch) {
                '<' -> {
                    writer.write('&'.code)
                    writer.write('l'.code)
                    writer.write('t'.code)
                    writer.write(';'.code)
                }
                '>' -> {
                    writer.write('&'.code)
                    writer.write('g'.code)
                    writer.write('t'.code)
                    writer.write(';'.code)
                }
                '&' -> {
                    writer.write('&'.code)
                    writer.write('a'.code)
                    writer.write('m'.code)
                    writer.write('p'.code)
                    writer.write(';'.code)
                }
                '"' -> {
                    writer.write('&'.code)
                    writer.write('q'.code)
                    writer.write('u'.code)
                    writer.write('o'.code)
                    writer.write('t'.code)
                    writer.write(';'.code)
                }
                '\'' -> {
                    writer.write('&'.code)
                    writer.write('a'.code)
                    writer.write('p'.code)
                    writer.write('o'.code)
                    writer.write('s'.code)
                    writer.write(';'.code)
                }
                else -> {
                    val unicode = ch.code
                    if (unicode < 32 || unicode > 126) {
                        writer.write('&'.code)
                        writer.write('#'.code)
                        writer.write('x'.code)
                        writer.write(Integer.toString(unicode, 16))
                        writer.write(';'.code)
                    } else {
                        writer.write(ch.code)
                    }
                }
            }
            i += 1
        }
    }

    /**
     * Scans an identifier from the current reader. The scanned identifier is
     * appended to `result`.
     *
     * @param result
     * The buffer in which the scanned identifier will be put.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `result != null`
     *  * The next character read from the reader is a valid first
     * character of an XML identifier.
     *
    </dd> *
    </dl> *
     *
     * <dl>
     * <dt>**Postconditions:**</dt>
     * <dd>
     *
     *  * The next character read from the reader won't be an
     * identifier character.
     *
    </dd> *
    </dl> *
     * <dl>
    </dl> */
    @Throws(IOException::class)
    protected fun scanIdentifier(result: StringBuffer) {
        while (true) {
            val ch = readChar()
            if ((ch < 'A' || ch > 'Z') && (ch < 'a' || ch > 'z')
                && (ch < '0' || ch > '9') && ch != '_' && ch != '.'
                && ch != ':' && ch != '-' && ch <= '\u007E'
            ) {
                unreadChar(ch)
                return
            }
            result.append(ch)
        }
    }

    /**
     * This method scans an identifier from the current reader.
     *
     * @return the next character following the whitespace.
     */
    @Throws(IOException::class)
    protected fun scanWhitespace(): Char {
        while (true) {
            val ch = readChar()
            when (ch) {
                ' ', '\t', '\n', '\r' -> {}
                else -> return ch
            }
        }
    }

    /**
     * This method scans an identifier from the current reader. The scanned
     * whitespace is appended to `result`.
     *
     * @return the next character following the whitespace.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `result != null`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun scanWhitespace(result: StringBuffer): Char {
        while (true) {
            val ch = readChar()
            when (ch) {
                ' ', '\t', '\n' -> result.append(ch)
                '\r' -> {}
                else -> return ch
            }
        }
    }

    /**
     * This method scans a delimited string from the current reader. The scanned
     * string without delimiters is appended to `string`.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `string != null`
     *  * the next char read is the string delimiter
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun scanString(string: StringBuffer) {
        val delimiter = readChar()
        if (delimiter != '\'' && delimiter != '"') {
            throw expectedInput("' or \"")
        }
        while (true) {
            val ch = readChar()
            if (ch == delimiter) {
                return
            } else if (ch == '&') {
                resolveEntity(string)
            } else {
                string.append(ch)
            }
        }
    }

    /**
     * Scans a #PCDATA element. CDATA sections and entities are resolved. The
     * next &lt; char is skipped. The scanned data is appended to
     * `data`.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `data != null`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun scanPCData(data: StringBuffer) {
        while (true) {
            var ch = readChar()
            if (ch == '<') {
                ch = readChar()
                if (ch == '!') {
                    checkCDATA(data)
                } else {
                    unreadChar(ch)
                    return
                }
            } else if (ch == '&') {
                resolveEntity(data)
            } else {
                data.append(ch)
            }
        }
    }

    /**
     * Scans a special tag and if the tag is a CDATA section, append its content
     * to `buf`.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `buf != null`
     *  * The first &lt; has already been read.
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun checkCDATA(buf: StringBuffer): Boolean {
        var ch = readChar()
        return if (ch != '[') {
            unreadChar(ch)
            skipSpecialTag(0)
            false
        } else if (!checkLiteral("CDATA[")) {
            skipSpecialTag(1) // one [ has already been read
            false
        } else {
            var delimiterCharsSkipped = 0
            while (delimiterCharsSkipped < 3) {
                ch = readChar()
                when (ch) {
                    ']' -> if (delimiterCharsSkipped < 2) {
                        delimiterCharsSkipped += 1
                    } else {
                        buf.append(']')
                        buf.append(']')
                        delimiterCharsSkipped = 0
                    }
                    '>' -> if (delimiterCharsSkipped < 2) {
                        var i = 0
                        while (i < delimiterCharsSkipped) {
                            buf.append(']')
                            i++
                        }
                        delimiterCharsSkipped = 0
                        buf.append('>')
                    } else {
                        delimiterCharsSkipped = 3
                    }
                    else -> {
                        var i = 0
                        while (i < delimiterCharsSkipped) {
                            buf.append(']')
                            i += 1
                        }
                        buf.append(ch)
                        delimiterCharsSkipped = 0
                    }
                }
            }
            true
        }
    }

    /**
     * Skips a comment.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * The first &lt;!-- has already been read.
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun skipComment() {
        var dashesToRead = 2
        while (dashesToRead > 0) {
            val ch = readChar()
            if (ch == '-') {
                dashesToRead -= 1
            } else {
                dashesToRead = 2
            }
        }
        if (readChar() != '>') {
            throw expectedInput(">")
        }
    }

    /**
     * Skips a special tag or comment.
     *
     * @param bracketLevel
     * The number of open square brackets ([) that have already been
     * read.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * The first &lt;! has already been read.
     *  * `bracketLevel >= 0`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun skipSpecialTag(bracketLevel: Int) {
        var bracketLevel = bracketLevel
        var tagLevel = 1 // <
        var stringDelimiter = '\u0000'
        if (bracketLevel == 0) {
            var ch = readChar()
            if (ch == '[') {
                bracketLevel += 1
            } else if (ch == '-') {
                ch = readChar()
                if (ch == '[') {
                    bracketLevel += 1
                } else if (ch == ']') {
                    bracketLevel -= 1
                } else if (ch == '-') {
                    skipComment()
                    return
                }
            }
        }
        while (tagLevel > 0) {
            val ch = readChar()
            if (stringDelimiter == '\u0000') {
                if (ch == '"' || ch == '\'') {
                    stringDelimiter = ch
                } else if (bracketLevel <= 0) {
                    if (ch == '<') {
                        tagLevel += 1
                    } else if (ch == '>') {
                        tagLevel -= 1
                    }
                }
                if (ch == '[') {
                    bracketLevel += 1
                } else if (ch == ']') {
                    bracketLevel -= 1
                }
            } else {
                if (ch == stringDelimiter) {
                    stringDelimiter = '\u0000'
                }
            }
        }
    }

    /**
     * Scans the data for literal text. Scanning stops when a character does not
     * match or after the complete text has been checked, whichever comes first.
     *
     * @param literal
     * the literal to check.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `literal != null`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun checkLiteral(literal: String): Boolean {
        val length = literal.length
        var i = 0
        while (i < length) {
            if (readChar() != literal[i]) {
                return false
            }
            i += 1
        }
        return true
    }

    /**
     * Reads a character from a reader.
     */
    @Throws(IOException::class)
    protected fun readChar(): Char {
        return if (charReadTooMuch != '\u0000') {
            val ch = charReadTooMuch
            charReadTooMuch = '\u0000'
            ch
        } else {
            val i = reader!!.read()
            if (i < 0) {
                throw unexpectedEndOfData()
            } else if (i == 10) {
                parserLineNr += 1
                '\n'
            } else {
                i.toChar()
            }
        }
    }

    /**
     * Scans an XML element.
     *
     * @param elt
     * The element that will contain the result.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * The first &lt; has already been read.
     *  * `elt != null`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun scanElement(elt: XMLElement) {
        val buf = StringBuffer()
        scanIdentifier(buf)
        val name = buf.toString()
        elt.name = name
        var ch = this.scanWhitespace()
        while (ch != '>' && ch != '/') {
            buf.setLength(0)
            unreadChar(ch)
            scanIdentifier(buf)
            val key = buf.toString()
            ch = this.scanWhitespace()
            if (ch != '=') {
                throw expectedInput("=")
            }
            unreadChar(this.scanWhitespace())
            buf.setLength(0)
            scanString(buf)
            elt.setAttribute(key, buf)
            ch = this.scanWhitespace()
        }
        if (ch == '/') {
            ch = readChar()
            if (ch != '>') {
                throw expectedInput(">")
            }
            return
        }
        buf.setLength(0)
        ch = this.scanWhitespace(buf)
        if (ch != '<') {
            unreadChar(ch)
            scanPCData(buf)
        } else {
            while (true) {
                ch = readChar()
                if (ch == '!') {
                    if (checkCDATA(buf)) {
                        scanPCData(buf)
                        break
                    } else {
                        ch = this.scanWhitespace(buf)
                        if (ch != '<') {
                            unreadChar(ch)
                            scanPCData(buf)
                            break
                        }
                    }
                } else {
                    if (ch != '/' || ignoreWhitespace) {
                        buf.setLength(0)
                    }
                    if (ch == '/') {
                        unreadChar(ch)
                    }
                    break
                }
            }
        }
        if (buf.length == 0) {
            while (ch != '/') {
                if (ch == '!') {
                    ch = readChar()
                    if (ch != '-') {
                        throw expectedInput("Comment or Element")
                    }
                    ch = readChar()
                    if (ch != '-') {
                        throw expectedInput("Comment or Element")
                    }
                    skipComment()
                } else {
                    unreadChar(ch)
                    val child = createAnotherElement()
                    scanElement(child)
                    elt.addChild(child)
                }
                ch = this.scanWhitespace()
                if (ch != '<') {
                    throw expectedInput("<")
                }
                ch = readChar()
            }
            unreadChar(ch)
        } else {
            if (ignoreWhitespace) {
                elt.content = buf.toString().trim { it <= ' ' }
            } else {
                elt.content = buf.toString()
            }
        }
        ch = readChar()
        if (ch != '/') {
            throw expectedInput("/")
        }
        unreadChar(this.scanWhitespace())
        if (!checkLiteral(name)) {
            throw expectedInput(name)
        }
        if (this.scanWhitespace() != '>') {
            throw expectedInput(">")
        }
    }

    /**
     * Resolves an entity. The name of the entity is read from the reader. The
     * value of the entity is appended to `buf`.
     *
     * @param buf
     * Where to put the entity value.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * The first &amp; has already been read.
     *  * `buf != null`
     *
    </dd> *
    </dl> *
     */
    @Throws(IOException::class)
    protected fun resolveEntity(buf: StringBuffer) {
        var ch = '\u0000'
        val keyBuf = StringBuffer()
        while (true) {
            ch = readChar()
            if (ch == ';') {
                break
            }
            keyBuf.append(ch)
        }
        val key = keyBuf.toString()
        if (key[0] == '#') {
            ch = try {
                if (key[1] == 'x') {
                    key.substring(2).toInt(16).toChar()
                } else {
                    key.substring(1).toInt(10).toChar()
                }
            } catch (e: NumberFormatException) {
                throw unknownEntity(key)
            }
            buf.append(ch)
        } else {
            val value = defaultEntities[key] ?: throw unknownEntity(key)
            buf.append(value)
        }
    }

    /**
     * Pushes a character back to the read-back buffer.
     *
     * @param ch
     * The character to push back.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * The read-back buffer is empty.
     *  * `ch != '\0'`
     *
    </dd> *
    </dl> *
     */
    protected fun unreadChar(ch: Char) {
        charReadTooMuch = ch
    }

    /**
     * Creates a parse exception for when an invalid valueset is given to a
     * method.
     *
     * @param name
     * The name of the entity.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *
    </dd> *
    </dl> *
     */
    protected fun invalidValueSet(name: String): XMLParseException {
        val msg = "Invalid value set (entity name = \"$name\")"
        return XMLParseException(this.name, parserLineNr, msg)
    }

    /**
     * Creates a parse exception for when an invalid value is given to a method.
     *
     * @param name
     * The name of the entity.
     * @param value
     * The value of the entity.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `value != null`
     *
    </dd> *
    </dl> *
     */
    protected fun invalidValue(name: String, value: String?): XMLParseException {
        val msg = ("Attribute \"" + name + "\" does not contain a valid "
                + "value (\"" + value + "\")")
        return XMLParseException(this.name, parserLineNr, msg)
    }

    /**
     * Creates a parse exception for when the end of the data input has been
     * reached.
     */
    protected fun unexpectedEndOfData(): XMLParseException {
        val msg = "Unexpected end of data reached"
        return XMLParseException(name, parserLineNr, msg)
    }

    /**
     * Creates a parse exception for when a syntax error occured.
     *
     * @param context
     * The context in which the error occured.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `context != null`
     *  * `context.length() > 0`
     *
    </dd> *
    </dl> *
     */
    protected fun syntaxError(context: String): XMLParseException {
        val msg = "Syntax error while parsing $context"
        return XMLParseException(name, parserLineNr, msg)
    }

    /**
     * Creates a parse exception for when the next character read is not the
     * character that was expected.
     *
     * @param charSet
     * The set of characters (in human readable form) that was
     * expected.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `charSet != null`
     *  * `charSet.length() > 0`
     *
    </dd> *
    </dl> *
     */
    protected fun expectedInput(charSet: String): XMLParseException {
        val msg = "Expected: $charSet"
        return XMLParseException(name, parserLineNr, msg)
    }

    /**
     * Creates a parse exception for when an entity could not be resolved.
     *
     * @param name
     * The name of the entity.
     *
     *
     * <dl>
     * <dt>**Preconditions:**</dt>
     * <dd>
     *
     *  * `name != null`
     *  * `name.length() > 0`
     *
    </dd> *
    </dl> *
     */
    protected fun unknownEntity(name: String): XMLParseException {
        val msg = "Unknown or invalid entity: &$name;"
        return XMLParseException(this.name, parserLineNr, msg)
    }

    companion object {
        /**
         * Serialization serial version ID.
         */
        const val serialVersionUID = 6685035139346394777L

        /**
         * Major version of NanoXML. Classes with the same major and minor version
         * are binary compatible. Classes with the same major version are source
         * compatible. If the major version is different, you may need to modify the
         * client source code.
         *
         * @see nanoxml.XMLElement.NANOXML_MINOR_VERSION
         */
        const val NANOXML_MAJOR_VERSION = 2

        /**
         * Minor version of NanoXML. Classes with the same major and minor version
         * are binary compatible. Classes with the same major version are source
         * compatible. If the major version is different, you may need to modify the
         * client source code.
         *
         * @see nanoxml.XMLElement.NANOXML_MAJOR_VERSION
         */
        const val NANOXML_MINOR_VERSION = 2

        /**
         * Conversion table for &amp;...; entities. The keys are the entity names
         * without the &amp; and ; delimiters.
         *
         * <dl>
         * <dt>**Invariants:**</dt>
         * <dd>
         *
         *  * The field is never `null`.
         *  * The field always contains the following associations:
         * "lt"&nbsp;=&gt;&nbsp;"&lt;", "gt"&nbsp;=&gt;&nbsp;"&gt;",
         * "quot"&nbsp;=&gt;&nbsp;"\"", "apos"&nbsp;=&gt;&nbsp;"'",
         * "amp"&nbsp;=&gt;&nbsp;"&amp;"
         *  * The keys are strings
         *  * The values are char arrays
         *
        </dd> *
        </dl> *
         */
        private val defaultEntities = Hashtable<String, CharArray>()
    }
}