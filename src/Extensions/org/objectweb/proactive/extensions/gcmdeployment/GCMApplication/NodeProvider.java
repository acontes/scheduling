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
package org.objectweb.proactive.extensions.gcmdeployment.GCMApplication;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.CommandBuilder;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import static org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers.GCMA_LOGGER;


public class NodeProvider {
    static Set<NodeProvider> nodeProviders = new HashSet<NodeProvider>();
    private String id;
    private Set<GCMDeploymentDescriptor> descriptors;
    private TechnicalServicesProperties technicalServicesProperties;

    static public Set<NodeProvider> getAllNodeProviders() {
        return nodeProviders;
    }

    public NodeProvider(String id) {
        this.id = id;
        this.descriptors = new HashSet<GCMDeploymentDescriptor>();
        this.technicalServicesProperties = new TechnicalServicesProperties();

        nodeProviders.add(this);
    }

    public void addGCMDeploymentDescriptor(GCMDeploymentDescriptor desc) {
        if (descriptors.contains(desc)) {
            GCMA_LOGGER.warn(desc.getDescriptorURL().toExternalForm() + " already the Node Provider named " +
                id);
        }
        descriptors.add(desc);
    }

    public void start(CommandBuilder commandBuilder, GCMApplicationInternal gcma) {
        for (GCMDeploymentDescriptor desc : descriptors) {
            desc.start(commandBuilder, gcma);
        }
    }

    public String getId() {
        return id;
    }

    protected Set<GCMDeploymentDescriptor> getDescriptors() {
        return descriptors;
    }

    public void setTechnicalServicesProperties(TechnicalServicesProperties providerTechnicalServices) {
        this.technicalServicesProperties = providerTechnicalServices;
    }

    public TechnicalServicesProperties getTechnicalServicesProperties() {
        return technicalServicesProperties;
    }

}
