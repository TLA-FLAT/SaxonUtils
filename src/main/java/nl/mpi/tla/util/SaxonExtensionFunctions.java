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

import com.twmacinta.util.MD5;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.om.StructuredQName; 
import net.sf.saxon.om.Sequence;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.tree.NamespaceNode;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.EmptySequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaxonExtensionFunctions {
    
    private static final Logger logger = LoggerFactory.getLogger(SaxonExtensionFunctions.class.getName());

    /**
     * Registers with Saxon 9.2+ all the extension functions 
     * <p>This method must be invoked once per TransformerFactory.
     *
     * @param factory the TransformerFactory pointing to
     * Saxon 9.2+ extension function registry 
     * (that is, a <tt>net.sf.saxon.Configuration</tt>). 
     * This object must be an instance of 
     * <tt>net.sf.saxon.TransformerFactoryImpl</tt>.
     */
    public static void registerAll(final Configuration config) {
        config.registerExtensionFunction(new FileExistsDefinition());
        config.registerExtensionFunction(new CheckURLDefinition());
        config.registerExtensionFunction(new UUIDDefinition());
        config.registerExtensionFunction(new EvaluateDefinition());
        config.registerExtensionFunction(new FindBagBaseDefinition());
        config.registerExtensionFunction(new FindFirstFileDefinition());
        config.registerExtensionFunction(new MD5Definition());
        config.registerExtensionFunction(new FileSizeDefinition());
    }

    // -----------------------------------------------------------------------
    // sx:fileExists
    // -----------------------------------------------------------------------

    public static final class FileExistsDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "fileExists");
        }

        public int getMinimumNumberOfArguments() {
            return 1;
        }

        public int getMaximumNumberOfArguments() {
            return 1;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.makeSequenceType(BuiltInAtomicType.ANY_URI, StaticProperty.EXACTLY_ONE) };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.SINGLE_BOOLEAN;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = null;
                    try {
                        final URI uri = new URI(((StringValue) arguments[0].head()).getStringValue());
                        final boolean exists = (new java.io.File(uri)).exists();
                        seq = (new XdmAtomicValue(exists)).getUnderlyingValue();
                    } catch (final Exception e) {
                        logger.error("sx:fileExists failed!", e);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // sx:fileExists
    // -----------------------------------------------------------------------

    public static final class UUIDDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "uuid");
        }

        public int getMinimumNumberOfArguments() {
            return 0;
        }

        public int getMaximumNumberOfArguments() {
            return 1;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.SINGLE_STRING };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.SINGLE_STRING;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = null;
                    try {
                        if (arguments.length > 0) {
                            final String uniq = ((StringValue) arguments[0].head()).getStringValue();
                            //System.err.println("DBG? sx:uuid("+uniq+")");
                        } else {
                            //System.err.println("DBG? sx:uuid(NULL)");
                        }
                        seq = (new XdmAtomicValue(UUID.randomUUID().toString())).getUnderlyingValue();
                    } catch (final Exception e) {
                        //logger.error("sx:uuid failed!", e);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // sx:checkURL
    // -----------------------------------------------------------------------

    public static final class CheckURLDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "checkURL");
        }

        public int getMinimumNumberOfArguments() {
            return 1;
        }

        public int getMaximumNumberOfArguments() {
            return 1;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.SINGLE_STRING };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.SINGLE_BOOLEAN;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = null;
                    try {
                        final String url = ((StringValue) arguments[0].head()).getStringValue();
                        boolean valid = true;
                        try {
                            final URL u = new URL(url);
                        } catch (final MalformedURLException e) {
                            valid = false;
                        }
                        seq = (new XdmAtomicValue(valid)).getUnderlyingValue();
                    } catch (final Exception e) {
                        logger.error("sx:checkURL failed!", e);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // sx:evaluate
    // -----------------------------------------------------------------------

    public static final class EvaluateDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "evaluate");
        }

        public int getMinimumNumberOfArguments() {
            return 2;
        }

        public int getMaximumNumberOfArguments() {
            return 3;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.SINGLE_NODE, SequenceType.SINGLE_STRING,
                    SequenceType.OPTIONAL_NODE };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.ANY_SEQUENCE;
        }

        public boolean dependsOnFocus() {
            return true;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = null;
                    try {
                        final NodeInfo node = (NodeInfo) arguments[0].head();
                        final StringValue path = (StringValue) arguments[1].head();
                        NodeInfo ns = node;
                        if (arguments.length == 3)
                            ns = (NodeInfo) arguments[2].head();
                        final Processor processor = new Processor(context.getConfiguration());
                        final XPathCompiler xpc = processor.newXPathCompiler();
                        final AxisIterator iter = ns.iterateAxis(AxisInfo.NAMESPACE);
                        NamespaceNode n = (NamespaceNode) iter.next();
                        while (n != null) {
                            xpc.declareNamespace(n.getLocalPart(), n.getStringValue());
                            n = (NamespaceNode) iter.next();
                        }
                        final XPathExecutable xpe = xpc.compile(path.getStringValue());
                        final XPathSelector xps = xpe.load();
                        xps.setContextItem(new XdmNode(node));
                        seq = xps.evaluate().getUnderlyingValue();
                    } catch (final SaxonApiException e) {
                        logger.error("sx:evaluate failed!", e);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // flt:findBagBase
    // -----------------------------------------------------------------------

    public static final class FindBagBaseDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("flat", "java:nl.mpi.tla.flat", "findBagBase");
        }

        public int getMinimumNumberOfArguments() {
            return 1;
        }

        public int getMaximumNumberOfArguments() {
            return 1;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.SINGLE_STRING };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.OPTIONAL_STRING;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        protected Optional<Path> findBagBase(final Path bag) {
            try (Stream<Path> stream = Files.find(bag, Integer.MAX_VALUE, new BiPredicate<Path, BasicFileAttributes>() {
                @Override
                public boolean test(Path path, BasicFileAttributes attr) {
                    return path.toString().endsWith(System.getProperty("file.separator") + "metadata"
                            + System.getProperty("file.separator") + "record.cmdi");
                }
            })) {
                return stream.findFirst();
            } catch (final Exception e) {
                logger.error("flat:findBagBase failed!", e);
            }
            return null;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = EmptySequence.getInstance();
                    try {
                        final String dir = arguments[0].head().getStringValue();
                        Path p = Paths.get(dir);
                        if (Files.isDirectory(p)) {
                            // look for: bag/???/metadata/record.cmdi
                            final Optional<Path> r = findBagBase(p);
                            if (r != null && r.isPresent()) {
                                p = r.get();
                                p = p.getParent().getParent();
                                seq = new XdmAtomicValue(p.toString()).getUnderlyingValue();
                            }
                        }
                    } catch (final Exception e) {
                        logger.error("flat:findBagBase failed!", e);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // sx:findFirstFile
    // -----------------------------------------------------------------------

    public static final class FindFirstFileDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "findFirstFile");
        }

        public int getMinimumNumberOfArguments() {
            return 2;
        }

        public int getMaximumNumberOfArguments() {
            return 2;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.OPTIONAL_STRING;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        protected Optional<Path> findFirstFile(final Path dir, final String fle) {
            try (Stream<Path> stream = Files.find(dir, Integer.MAX_VALUE, new BiPredicate<Path, BasicFileAttributes>() {
                @Override
                public boolean test(Path path, BasicFileAttributes attr) {
                    return path.toString().endsWith(fle);
                }
            })) {
                return stream.findFirst();
            } catch (final Exception e) {
                logger.error("sx:findFirstFile failed!", e);
            }
            return null;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = EmptySequence.getInstance();
                    try {
                        final String dir = arguments[0].head().getStringValue();
                        Path p = Paths.get(dir);
                        if (Files.isDirectory(p)) {
                            final String fle = arguments[1].head().getStringValue();
                            // look for: bag/???/metadata/record.cmdi
                            final Optional<Path> r = findFirstFile(p, fle);
                            if (r != null && r.isPresent()) {
                                p = r.get();
                                seq = new XdmAtomicValue(p.toString()).getUnderlyingValue();
                            }
                        }
                    } catch (final Exception e) {
                        logger.error("sx:findFirstFile failed!", e);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // sx:md5
    // -----------------------------------------------------------------------

    public static final class MD5Definition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "md5");
        }

        public int getMinimumNumberOfArguments() {
            return 1;
        }

        public int getMaximumNumberOfArguments() {
            return 1;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.makeSequenceType(BuiltInAtomicType.ANY_URI, StaticProperty.EXACTLY_ONE) };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.SINGLE_STRING;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = null;
                    try {
                        final URI uri = new URI(((StringValue) arguments[0].head()).getStringValue());
                        final String hash = MD5.asHex(MD5.getHash(new java.io.File(uri)));
                        seq = (new XdmAtomicValue(hash)).getUnderlyingValue();
                    } catch (final Exception e) {
                        System.err.println("ERR: [" + ((StringValue) arguments[0].head()).getStringValue() + "]:"
                                + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    return seq;
                }
            };
        }
    }

    // -----------------------------------------------------------------------
    // sx:fileSize
    // -----------------------------------------------------------------------

    public static final class FileSizeDefinition extends ExtensionFunctionDefinition {
        public StructuredQName getFunctionQName() {
            return new StructuredQName("sx", "java:nl.mpi.tla.saxon", "fileSize");
        }

        public int getMinimumNumberOfArguments() {
            return 1;
        }

        public int getMaximumNumberOfArguments() {
            return 1;
        }

        public SequenceType[] getArgumentTypes() {
            return new SequenceType[] { SequenceType.makeSequenceType(BuiltInAtomicType.ANY_URI, StaticProperty.EXACTLY_ONE) };
        }

        public SequenceType getResultType(final SequenceType[] suppliedArgTypes) {
            return SequenceType.SINGLE_INTEGER;
        }

        public boolean dependsOnFocus() {
            return false;
        }

        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override
                public Sequence call(final XPathContext context, final Sequence[] arguments) throws XPathException {
                    Sequence seq = null;
                    try {
                        final URI uri = new URI(((StringValue) arguments[0].head()).getStringValue());
                        final File file = new java.io.File(uri);
                        seq = (new XdmAtomicValue(file.length())).getUnderlyingValue();
                    } catch (final Exception e) {
                        System.err.println("ERR: ["+((StringValue) arguments[0].head()).getStringValue()+"]:"+e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    return seq;
                }
            };
        }
    }
}
