/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.component.request;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.api.control.LifeCycleController;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.core.body.UniversalBody;
import org.objectweb.proactive.core.body.future.FutureResult;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.body.request.RequestImpl;
import org.objectweb.proactive.core.body.request.ServeException;
import org.objectweb.proactive.core.component.Constants;
import org.objectweb.proactive.core.component.Fractive;
import org.objectweb.proactive.core.component.ProActiveInterface;
import org.objectweb.proactive.core.component.Utils;
import org.objectweb.proactive.core.component.body.ComponentBody;
import org.objectweb.proactive.core.component.body.ComponentBodyImpl;
import org.objectweb.proactive.core.component.controller.ComponentParametersController;
import org.objectweb.proactive.core.component.identity.ProActiveComponentImpl;
import org.objectweb.proactive.core.component.interception.InputInterceptor;
import org.objectweb.proactive.core.component.representative.ItfID;
import org.objectweb.proactive.core.component.type.ProActiveInterfaceType;
import org.objectweb.proactive.core.mop.MethodCall;
import org.objectweb.proactive.core.mop.MethodCallExecutionFailedException;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


/**
 * Method calls to components are actually reified calls, and {@link ComponentRequest}
 * contains a reification of the call.
 * <p>
 * This class handles the tagging of the call (a component call), and the
 * redispatching to the targeted component metaobject, interface reference, base
 * object. It also allows pre and post processing of functional invocations with
 * inputInterceptors.
 *
 * @author Matthieu Morel
 */
public class ComponentRequestImpl extends RequestImpl
    implements ComponentRequest, Serializable {
    protected static Logger logger = ProActiveLogger.getLogger(Loggers.COMPONENTS_REQUESTS);

    //private int shortcutCounter = 0;
    //private Shortcut shortcut;
    private final Class<?> declaringClass;
    private final Class<?> targetClass;

    // priorities for NF requests (notably when using filters on functional requests) : 
    //private short priority=ComponentRequest.STRICT_FIFO_PRIORITY;
    public ComponentRequestImpl(MethodCall methodCall, UniversalBody sender,
        boolean isOneWay, long nextSequenceID) {
        super(methodCall, sender, isOneWay, nextSequenceID);
        declaringClass = methodCall.getReifiedMethod().getDeclaringClass();
        targetClass = methodCall.getReifiedMethod().getDeclaringClass();
    }

    public ComponentRequestImpl(Request request) {
        super(request.getMethodCall(), request.getSender(), request.isOneWay(),
            request.getSequenceNumber());
        declaringClass = methodCall.getReifiedMethod().getDeclaringClass();
        targetClass = methodCall.getReifiedMethod().getDeclaringClass();
    }

    /**
     * redirects the call to the adequate component metaobject : either to a controller, through the chain of controllers, to
     * a functional interface in the case of a composite (no preprocessing in that case), or directly executes the invocation
     * on the base object if this component is a primitive component and the invocation is a functional invocation.
     */
    @Override
    protected FutureResult serveInternal(Body targetBody)
        throws ServeException {
        Object result = null;
        Throwable exception = null;

        if (logger.isDebugEnabled()) {
            try {
                logger.debug("invocation on method [" + methodCall.getName() +
                    "] of interface [" +
                    methodCall.getComponentMetadata().getComponentInterfaceName() +
                    "] on component : [" +
                    ((ComponentParametersController) ((ComponentBodyImpl) targetBody)
                     .getProActiveComponentImpl()
                     .getFcInterface(Constants.COMPONENT_PARAMETERS_CONTROLLER)).getComponentParameters()
                     .getName() + "]");
            } catch (NoSuchInterfaceException e) {
                e.printStackTrace();
            }
        }

        try {
            if (isControllerRequest()) {
                result = ((ProActiveComponentImpl) ((ComponentBodyImpl) targetBody)
                          .getProActiveComponentImpl()).getControllerRequestHandler()
                          .handleRequest(this);
            } else {
                if (((ComponentBodyImpl) targetBody).getProActiveComponentImpl() != null) {
                    interceptBeforeInvocation(targetBody);

                    String hierarchical_type = Fractive.getComponentParametersController(((ComponentBodyImpl) targetBody).getProActiveComponentImpl())
                                                       .getComponentParameters()
                                                       .getHierarchicalType();

                    // gather: interception managed with non-transformed incoming requests
                    ProActiveInterface itf = (ProActiveInterface) ((ComponentBody) targetBody).getProActiveComponentImpl()
                                                                   .getFcInterface(methodCall.getComponentMetadata()
                                                                                             .getComponentInterfaceName());
                    ProActiveInterfaceType itfType = (ProActiveInterfaceType) itf.getFcItfType();
                    if (itfType.isFcGathercastItf() &&
                            (!getMethodCall().getComponentMetadata()
                                      .getSenderItfID()
                                      .equals(new ItfID(
                                    itfType.getFcItfName(), targetBody.getID())))) {
                        // delegate to gather controller, except for self requests
                        result = Fractive.getGathercastController(((ComponentBodyImpl) targetBody).getProActiveComponentImpl())
                                         .handleRequestOnGatherItf(this);
                    }
                    // if the component is a composite , forward to functional interface 
                    else if (hierarchical_type.equals(Constants.COMPOSITE)) {
                        //						// forward to functional interface whose name is given as a parameter in the method call
                        try {
                            if (getShortcut() != null) {
                                // TODO_M allow stopping shortcut here
                            }
                            // executing on connected server interface
                            result = methodCall.execute((ProActiveInterface) (((ComponentBodyImpl) targetBody).getProActiveComponentImpl()).getFcInterface(
                                        methodCall.getComponentMetadata()
                                                  .getComponentInterfaceName()));
                        } catch (IllegalArgumentException e) {
                            throw new ServeException("could not reify method call : ",
                                e);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw new ServeException("could not reify method call : ",
                                e);
                        }
                    } else {
                        // the component is a primitive
                        // directly execute the method on the active object
                        if (logger.isDebugEnabled()) {
                            if (getShortcutLength() > 0) {
                                logger.debug("request has crossed " +
                                    (getShortcutLength() - 1) +
                                    " membranes before reaching a primitive component");
                            }
                        }
                        result = methodCall.execute(targetBody.getReifiedObject());
                    }
                    interceptAfterInvocation(targetBody);
                } else {
                    throw new ServeException(
                        "trying to execute a component method on an object that is not a component");
                }
            }
        } catch (NoSuchInterfaceException nsie) {
            throw new ServeException("cannot serve request : problem accessing a component controller",
                nsie);
        } catch (MethodCallExecutionFailedException e) {
            throw new ServeException("serve method " +
                methodCall.getReifiedMethod().toString() + " failed", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            exception = e.getTargetException();
            if (logger.isDebugEnabled()) {
                logger.debug("Serve method " +
                    methodCall.getReifiedMethod().getName() + " failed: ", e);
            } else {
                logger.info("Serve method " +
                    methodCall.getReifiedMethod().getName() +
                    " failed; throws exception with the following message:" +
                    e.getMessage() +
                    " Activate debug logger level for more information.");
            }

            if (isOneWay) {
                throw new ServeException("serve method " +
                    methodCall.getReifiedMethod().toString() + " failed",
                    exception);
            }
        }

        return new FutureResult(result, exception);
    }

    // intercept and delegate for preprocessing from the inputInterceptors 
    private void interceptBeforeInvocation(Body targetBody) {
        if (methodCall.getReifiedMethod() != null) {
            List inputInterceptors = ((ComponentBodyImpl) targetBody).getProActiveComponentImpl()
                                      .getInputInterceptors();
            Iterator it = inputInterceptors.iterator();
            while (it.hasNext()) {
                try {
                    InputInterceptor interceptor = (InputInterceptor) it.next();
                    interceptor.beforeInputMethodInvocation(methodCall);
                } catch (NullPointerException e) {
                    logger.error("could not intercept invocation : " +
                        e.getMessage());
                }
            }
        }
    }

    // intercept and delegate for postprocessing from the inputInterceptors 
    private void interceptAfterInvocation(Body targetBody) {
        if (methodCall.getReifiedMethod() != null) {
            if (((ComponentBodyImpl) targetBody).getProActiveComponentImpl() != null) {
                List interceptors = ((ComponentBodyImpl) targetBody).getProActiveComponentImpl()
                                     .getInputInterceptors();

                // use inputInterceptors in reverse order after invocation
                ListIterator it = interceptors.listIterator();

                // go to the end of the list first
                while (it.hasNext()) {
                    it.next();
                }
                while (it.hasPrevious()) {
                    ((InputInterceptor) it.previous()).afterInputMethodInvocation(methodCall);
                }
            }
        }
    }

    /*
     * @see org.objectweb.proactive.core.component.request.ComponentRequest#isControllerRequest()
     */
    public boolean isControllerRequest() {
        // according to the Fractal spec v2.0 , section 4.1
        return Utils.isControllerInterfaceName(methodCall.getComponentMetadata()
                                                         .getComponentInterfaceName());
    }

    /*
     * @see org.objectweb.proactive.core.component.request.ComponentRequest#isStopFcRequest()
     */
    public boolean isStopFcRequest() {
        return (declaringClass.equals(LifeCycleController.class) &&
        "stopFc".equals(getMethodName()));
    }

    /*
     * @see org.objectweb.proactive.core.component.request.ComponentRequest#isStartFcRequest()
     */
    public boolean isStartFcRequest() {
        return (declaringClass.equals(LifeCycleController.class) &&
        "startFc".equals(getMethodName()));
    }

    @Override
    public void notifyReception(UniversalBody bodyReceiver)
        throws IOException {
        if (getShortcut() != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("notifying reception of method " +
                    methodCall.getName());
            }
            Shortcut shortcut = getShortcut();
            shortcut.updateDestination(bodyReceiver.getRemoteAdapter());
            shortcut.getSender().createShortcut(shortcut);

            ((ComponentBodyImpl) bodyReceiver).keepShortcut(shortcut);
        }
        super.notifyReception(bodyReceiver);
    }

    public void shortcutNotification(UniversalBody sender,
        UniversalBody intermediate) {
        methodCall.getComponentMetadata()
                  .shortcutNotification(sender, intermediate);
    }

    public Shortcut getShortcut() {
        return methodCall.getComponentMetadata().getShortcut();
    }

    public int getShortcutLength() {
        return ((getShortcut() == null) ? 0 : getShortcut().length());
    }

    /*
     * @see org.objectweb.proactive.core.component.request.ComponentRequest#getNFPriority()
     */
    public short getPriority() {
        return methodCall.getComponentMetadata().getPriority();
    }

    /*
     * @see org.objectweb.proactive.core.component.request.ComponentRequest#getTargetClass()
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }
}
