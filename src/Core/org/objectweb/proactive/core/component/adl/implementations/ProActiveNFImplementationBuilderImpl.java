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
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.component.adl.implementations;

import java.util.List;
import java.util.Map;

import org.objectweb.fractal.api.Component;
import org.objectweb.fractal.api.Type;
import org.objectweb.fractal.api.type.ComponentType;
import org.objectweb.fractal.util.Fractal;
import org.objectweb.proactive.core.component.Constants;
import org.objectweb.proactive.core.component.ContentDescription;
import org.objectweb.proactive.core.component.ControllerDescription;
import org.objectweb.proactive.core.component.adl.nodes.VirtualNode;
import org.objectweb.proactive.core.component.factory.ProActiveGenericFactory;
import org.objectweb.proactive.core.group.Group;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;


/**
 * @author The ProActive Team
 */
public class ProActiveNFImplementationBuilderImpl extends ProActiveImplementationBuilderImpl {
    @Override
    public Object createComponent(Object type, String name, String definition,
            ControllerDescription controllerDesc, ContentDescription contentDesc, VirtualNode adlVN,
            Map context) throws Exception {
        ObjectsContainer obj = commonCreation(type, name, definition, contentDesc, adlVN, context);

        return createNFComponent(type, obj.getDvn(), controllerDesc, contentDesc, adlVN, obj
                .getBootstrapComponent());
    }

    private Component createNFComponent(Object type,
            org.objectweb.proactive.core.descriptor.data.VirtualNode deploymentVN,
            ControllerDescription controllerDesc, ContentDescription contentDesc, VirtualNode adlVN,
            Component bootstrap) throws Exception {
        Component result;

        // FIXME : exhaustively specify the behavior
        if ((deploymentVN != null) && VirtualNode.MULTIPLE.equals(adlVN.getCardinality()) &&
            controllerDesc.getHierarchicalType().equals(Constants.PRIMITIVE) && !contentDesc.uniqueInstance()) {

            Object instanceList = newNFcInstanceAsList(bootstrap, (ComponentType) type, controllerDesc,
                    contentDesc, deploymentVN);
            result = (Component) ((Group<?>) instanceList).getGroupByType();
        } else {
            result = newNFcInstance(bootstrap, (ComponentType) type, controllerDesc, contentDesc,
                    deploymentVN);
        }

        //        registry.addComponent(result); // the registry can handle groups
        return result;
    }

    private List<Component> newNFcInstanceAsList(Component bootstrap, Type type,
            ControllerDescription controllerDesc, ContentDescription contentDesc,
            org.objectweb.proactive.core.descriptor.data.VirtualNode virtualNode) throws Exception {

        ProActiveGenericFactory genericFactory = (ProActiveGenericFactory) Fractal
                .getGenericFactory(bootstrap);

        if (virtualNode == null) {
            return genericFactory.newNFcInstanceAsList(type, controllerDesc, contentDesc, (Node[]) null);
        }
        try {
            virtualNode.activate();
            return genericFactory.newNFcInstanceAsList(type, controllerDesc, contentDesc, virtualNode
                    .getNodes());
        } catch (NodeException e) {
            throw new InstantiationException(
                "could not instantiate components due to a deployment problem : " + e.getMessage());
        }
    }

    private Component newNFcInstance(Component bootstrap, Type type, ControllerDescription controllerDesc,
            ContentDescription contentDesc,
            org.objectweb.proactive.core.descriptor.data.VirtualNode virtualNode) throws Exception {

        ProActiveGenericFactory genericFactory = (ProActiveGenericFactory) Fractal
                .getGenericFactory(bootstrap);

        if (virtualNode == null) {
            return genericFactory.newNFcInstance(type, controllerDesc, contentDesc, (Node) null);
        }
        try {
            virtualNode.activate();
            if (virtualNode.getNodes().length == 0) {
                throw new InstantiationException(
                    "Cannot create component on virtual node as no node is associated with this virtual node");
            }
            return genericFactory.newNFcInstance(type, controllerDesc, contentDesc, virtualNode.getNode());
        } catch (NodeException e) {
            throw new InstantiationException(
                "could not instantiate components due to a deployment problem : " + e.getMessage());
        }
    }

}
