/* 
 * Copyright (C) 2015-2018 The Language Archive, Meertens Institute
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.mpi.tla.util;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import net.sf.saxon.Configuration;
import net.sf.saxon.Transform;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmFunctionItem;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.tree.wrapper.VirtualNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author menzowi
 */
public class Saxon extends Transform {
    
    private static final Logger logger = LoggerFactory.getLogger(Saxon.class.getName());
    
    /**
     * The Saxon processor from which there should be only one. Any Saxon
     * related instance, e.g., an XML document or an XSLT transform, should
     * share this processor. Otherwise Saxon will complain as it can't used
     * shared constructs, like the NamePool.
     */
    static private Processor sxProcessor = null;
    /**
     * The Saxon XSLT compiler.
     */
    static private XsltCompiler sxXsltCompiler = null;
    /**
     * The Saxon Document Builder
     */
    static private DocumentBuilder sxDocumentBuilder = null;

    /**
     * Get a Saxon processor, i.e., just-in-time create the Singleton.
     *
     * @return The Saxon processor
     */
    public static synchronized Processor getProcessor() {
        if (sxProcessor == null) {
            sxProcessor = new Processor(false);
            try {
                SaxonExtensionFunctions.registerAll(sxProcessor.getUnderlyingConfiguration());
            } catch (final Exception e) {
                logger.error("Couldn't register the Saxon extension functions!", e);
            }
            // Configuration sxConfig = sxProcessor.getUnderlyingConfiguration();
            // sxConfig.setMessageEmitterClass("net.sf.saxon.serialize.MessageWarner");
        }
        return sxProcessor;
    }

    public static synchronized XsltCompiler getXsltCompiler() {
        if (sxXsltCompiler == null) {
            sxXsltCompiler = getProcessor().newXsltCompiler();
        }
        return sxXsltCompiler;
    }

    public static synchronized XPathCompiler getXPathCompiler() {
        return getProcessor().newXPathCompiler();
    }

    public static synchronized XQueryCompiler getXQueryCompiler() {
        return getProcessor().newXQueryCompiler();
    }

    public static synchronized DocumentBuilder getDocumentBuilder() {
        if (sxDocumentBuilder == null) {
            sxDocumentBuilder = getProcessor().newDocumentBuilder();
        }
        return sxDocumentBuilder;
    }

    /**
     * Load an XML document.
     *
     * @param src The source of the document.
     * @return A Saxon XDM node
     * @throws SaxonApiException
     */
    static public XdmNode buildDocument(final Source src) throws SaxonApiException {
        return getDocumentBuilder().build(src);
    }

    /**
     * Load an XML into a DOM.
     *
     * @param src The source of the document.
     * @return A DOM document node
     * @throws Exception
     */
    static public Document buildDOM(final File src) throws Exception {
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);
        doc.setDocumentURI(src.toURI().toString());
        return doc;
    }

    /**
     * Load JSON.
     *
     * @param json The JSON
     * @return A Saxon XDM node
     * @throws SaxonApiException
     */
    static public XdmNode parseJson(final String json) throws SaxonApiException {
        XdmFunctionItem parseJsonFn = XdmFunctionItem.getSystemFunction(Saxon.getProcessor(), new QName("http://www.w3.org/2005/xpath-functions","json-to-xml"), 1);
        XdmValue val = parseJsonFn.call(Saxon.getProcessor(), new XdmAtomicValue(json));
        if (!(val instanceof net.sf.saxon.s9api.XdmNode)) {
          throw new SaxonApiException("Saxon.parseJson() resulted in "+val.getClass().getCanonicalName()+" != net.sf.saxon.s9api.XdmNode!");
        }
        return (XdmNode)val;
        }

    /**
     * Compile an XLST document. To use compiled XSLT document use the load() method
     * to turn it into a XsltTransformer.
     *
     * @param xslStylesheet
     * @return An Saxon XSLT executable, which can be shared.
     * @throws SaxonApiException
     */
    static public XsltExecutable buildTransformer(final XdmNode xslStylesheet) throws SaxonApiException {
        return getXsltCompiler().compile(xslStylesheet.asSource());
    }

    /**
     * Convenience method to build a XSLT transformer from a resource.
     *
     * @param uri The location of the resource
     * @return An executable XSLT
     * @throws Exception
     */
    static public XsltExecutable buildTransformer(final File file) throws SaxonApiException {
        return buildTransformer(buildDocument(new javax.xml.transform.stream.StreamSource(file)));
    }

    /**
     * Convenience method to build a XSLT transformer from a resource.
     *
     * @param uri The location of the resource
     * @return An executable XSLT
     * @throws Exception
     */
    static public XsltExecutable buildTransformer(final URL url) throws SaxonApiException {
        return buildTransformer(buildDocument(new javax.xml.transform.stream.StreamSource(url.toExternalForm())));
    }

    /**
     * Convenience method to build a XSLT transformer from a resource.
     *
     * @param uri The location of the resource
     * @return An executable XSLT
     * @throws Exception
     */
    static public XsltExecutable buildTransformer(final InputStream stream) throws SaxonApiException {
        return buildTransformer(buildDocument(new javax.xml.transform.stream.StreamSource(stream)));
    }

    /**
     * Wrap a DOM Node in a Saxon XDM node.
     */
    static public XdmNode wrapNode(final Node node) {
        return getDocumentBuilder().wrap(node);
    }

    /**
     * Unwrap a DOM Node from a Saxon XDM node.
     */
    static public Node unwrapNode(final XdmNode node) {
        return (Node) ((VirtualNode) node.getUnderlyingNode()).getUnderlyingNode();
    }
    /* XPath2 utilities */

    static public XPathSelector xpathCompile(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        try {
            XPathCompiler xpc = getXPathCompiler();
            if (vars != null) {
                for (final String name : vars.keySet()) {
                    xpc.declareVariable(new QName(name));
                }
            }
            if (nss != null) {
                for (final String prefix : nss.keySet()) {
                    xpc.declareNamespace(prefix, nss.get(prefix));
                }
            }
            final XPathSelector xps = xpc.compile(xp).load();
            xps.setContextItem(ctxt);
            if (vars != null) {
                for (final String name : vars.keySet()) {
                    xps.setVariable(new QName(name), vars.get(name));
                }
            }
            return xps;
        } catch (final SaxonApiException e) {
            logger.error("xpathCompile: xpath[" + xp + "] failed: " + e, e);
            throw e;
        }
    }

    static public XPathSelector xpathCompile(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpathCompile(ctxt, xp, null, null);
    }

    static public XdmValue xpath(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        return xpathCompile(ctxt, xp, vars, nss).evaluate();
    }

    static public XdmValue xpath(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return xpath(ctxt, xp, vars, null);
    }

    static public XdmValue xpath(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpath(ctxt, xp, null);
    }

    static public Iterator<XdmItem> xpathIterator(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        return xpathCompile(ctxt, xp, vars, nss).iterator();
    }

    static public Iterator<XdmItem> xpathIterator(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return xpathIterator(ctxt, xp, vars, null);
    }

    static public Iterator<XdmItem> xpathIterator(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpathIterator(ctxt, xp, null);
    }

    static public List<XdmItem> xpathList(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        return iterator2List(xpathIterator(ctxt, xp, vars, nss));
    }

    static public List<XdmItem> xpathList(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return xpathList(ctxt, xp, vars, null);
    }

    static public List<XdmItem> xpathList(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpathList(ctxt, xp, null);
    }

    static public XdmItem xpathSingle(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        return xpathCompile(ctxt, xp, vars, nss).evaluateSingle();
    }

    static public XdmItem xpathSingle(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return xpathSingle(ctxt, xp, vars, null);
    }

    static public XdmItem xpathSingle(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpathSingle(ctxt, xp, null);
    }

    static public String xpath2string(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        String res = "";
        for (final Iterator iter = xpathIterator(ctxt, xp, vars, nss); iter.hasNext();) {
            res += ((XdmItem) iter.next()).getStringValue();
        }
        return res;
    }

    static public String xpath2string(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return xpath2string(ctxt, xp, vars, null);
    }

    static public String xpath2string(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpath2string(ctxt, xp, null, null);
    }

    static public boolean xpath2boolean(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        return xpathCompile(ctxt, xp, vars, nss).effectiveBooleanValue();
    }

    static public boolean xpath2boolean(final XdmItem ctxt, final String xp, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return xpath2boolean(ctxt, xp, vars, null);
    }

    static public boolean xpath2boolean(final XdmItem ctxt, final String xp) throws SaxonApiException {
        return xpath2boolean(ctxt, xp, null, null);
    }

    /* Attributes */

    static public boolean hasAttribute(final XdmItem ctxt, final String attr) throws SaxonApiException {
        return Saxon.xpath2boolean(ctxt, "exists(@" + attr + ")");
    }

    /* Attribute Value Templates */
    static protected Pattern AVTPattern = Pattern.compile("\\{+.*?\\}+");

    static public String avt(final String avt, final XdmItem ctxt, final Map<String, XdmValue> vars)
            throws SaxonApiException {
        return avt(avt, ctxt, vars, null, true);
    }

    static public String avt(final String avt, final XdmItem ctxt, final Map<String, XdmValue> vars,
            final Map<String, String> nss) throws SaxonApiException {
        return avt(avt, ctxt, vars, nss, true);
    }

    static public String avt(final String avt, final XdmItem ctxt, final Map<String, XdmValue> vars,
            final boolean unescape) throws SaxonApiException {
        return avt(avt, ctxt, vars, null, unescape);
    }

    static public String avt(final String avt, final XdmItem ctxt, final Map<String, XdmValue> vars,
            final Map<String, String> nss, final boolean unescape) throws SaxonApiException {
        String res = "";
        final Matcher AVTMatcher = AVTPattern.matcher(avt);
        int start = 0;
        while (AVTMatcher.find()) {
            if (start < AVTMatcher.start())
                res += avt.substring(start, AVTMatcher.start());
            final String grp = AVTMatcher.group();
            if (grp.startsWith("{{") && grp.endsWith("}}")) {
                if (unescape)
                    res += grp.substring(1, grp.length() - 1);
                else
                    res += grp;
            } else {
                try {
                    res += Saxon.xpath2string(ctxt, grp.substring(1, grp.length() - 1), vars, nss);
                } catch (final Exception e) {
                    logger.error("avt[" + avt + "] failed: " + e, e);
                    throw e;
                }
            }
            start = AVTMatcher.end();
        }
        if (start < avt.length())
            res += avt.substring(start, avt.length());
        if (start > 0)
            logger.debug("AVT result[" + res + "]");
        return res;
    }

    // save an XML

    static public void save(final Source source, final File result) throws SaxonApiException {
        try {
            final XsltTransformer transformer = buildTransformer(Saxon.class.getResource("/identity.xsl")).load();
            transformer.setSource(source);
            transformer.setDestination(getProcessor().newSerializer(result));
            transformer.transform();
            transformer.close();
        } catch (final Exception ex) {
            throw new SaxonApiException(ex);
        }
    }

    static public void save(final XdmDestination dest, final File result) throws SaxonApiException {
        Saxon.save(dest.getXdmNode().asSource(), result);
    }

    static public String toString(final Source source) throws SaxonApiException {
        try {
            final XsltTransformer transformer = buildTransformer(Saxon.class.getResource("/identity.xsl")).load();
            transformer.setSource(source);
            final StringWriter str = new StringWriter();
            transformer.setDestination(getProcessor().newSerializer(str));
            transformer.transform();
            transformer.close();
            return str.toString();
        } catch (final Exception ex) {
            throw new SaxonApiException(ex);
        }
    }

    // Turn an XdmItem Iterator into a List
    static public List<XdmItem> iterator2List(final Iterator<XdmItem> iter) {
        final List<XdmItem> list = new ArrayList<>();
        iter.forEachRemaining(list::add);
        return list;
    }

    // Extension of default Saxon CLI with our extension functions
    protected void initializeConfiguration(final Configuration config) {
        SaxonExtensionFunctions.registerAll(config);
    }

    public static void main(final String args[]) {
        final Saxon saxon = new Saxon();
        saxon.doTransform(args, "TLA Saxon");
    }
    
}
