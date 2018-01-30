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
package nl.mpi.tla.util.saxon;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 *
 * @author menzowi
 */
public class Listener implements MessageListener, ErrorListener {
    
    protected Logger logger = LoggerFactory.getLogger(nl.mpi.tla.util.Saxon.class.getName());
    
    protected String type = "Saxon";
    protected String mdc  = "sip";
    protected String id   = null;

    public Listener() {
        this(null,null,null,null);
    }
    
    public Listener(Logger logger) {
        this(logger,null,null,null);
    }

    public Listener(Logger logger,String type) {
        this(logger,type,null,null);
    }

    public Listener(String type) {
        this(null,type,null,null);
    }

    public Listener(Logger logger,String type,String id) {
        this(logger,type,null,id);
    }
    
    public Listener(String type,String id) {
        this(null,type,null,id);
    }
    
    public Listener(String type,String mdc,String id) {
        this(null,type,mdc,id);
    }

    public Listener(Logger logger,String type,String mdc,String id) {
        if (logger != null)
            this.logger = logger;
        if (type != null)
            this.type = type;
        if (mdc != null)
            this.mdc = mdc;
        if (id != null)
            this.id = id;
    }
    
    protected void setID() {
        if (this.id != null)
            if (MDC.get(mdc)==null)
                MDC.put(mdc,this.id);
    }
    
    protected boolean handleMessage(String msg, String loc, Exception e) {
        if (msg.startsWith("INF: "))
            logger.info(type+": "+msg.replace("INF: ", ""));
        else if (msg.startsWith("WRN: "))
            logger.warn(type+"["+loc+"]: "+msg.replace("WRN: ", ""), e);
        else if (msg.startsWith("ERR: "))
            logger.error(type+"["+loc+"]: "+msg.replace("ERR: ", ""), e);
        else if (msg.startsWith("DBG: "))
            logger.debug(type+"["+loc+"]: "+msg.replace("DBG: ", ""), e);
        else
            return false;
        return true;
    }
    
    protected boolean handleException(TransformerException te) {
        return handleMessage(te.getMessage(), te.getLocationAsString(), te);
    }

    @Override
    public void warning(TransformerException te) throws TransformerException {
        setID();
        if (!handleException(te))
            logger.warn(type+": "+te.getMessageAndLocation(), te);
    }

    @Override
    public void error(TransformerException te) throws TransformerException {
        setID();
        if (!handleException(te))
            logger.error(type+": "+te.getMessageAndLocation(), te);
    }

    @Override
    public void fatalError(TransformerException te) throws TransformerException {
        setID();
        if (!handleException(te))
            logger.error(type+": "+te.getMessageAndLocation(), te);
    }
    
    protected String getLocation(SourceLocator sl) {
        if (sl.getColumnNumber()<0)
            return "-1";
        return sl.getSystemId()+":"+sl.getLineNumber()+":"+sl.getColumnNumber();
    }

    @Override
    public void message(XdmNode xn, boolean bln, SourceLocator sl) {
        setID();
        if (!handleMessage(xn.getStringValue(),getLocation(sl),null)) {
            if (bln)
                logger.error(type+"["+getLocation(sl)+"]: "+xn.getStringValue());
            else
                logger.info(type+"["+getLocation(sl)+"]: "+xn.getStringValue());
        }
    }
}
