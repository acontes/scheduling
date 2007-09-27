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
package org.objectweb.proactive.examples.flowshop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.ProDeployment;
import org.objectweb.proactive.api.ProFuture;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.branchnbound.ProActiveBranchNBound;
import org.objectweb.proactive.extensions.branchnbound.core.Manager;
import org.objectweb.proactive.extensions.branchnbound.core.Result;
import org.objectweb.proactive.extensions.branchnbound.core.Task;
import org.objectweb.proactive.extensions.branchnbound.core.queue.BasicQueueImpl;


/**
 * This main class provide a launcher of the FlowShop application.
 * Some Java properties can be used to activate options :
 * - flowshoptask.com
 *   if this properties are setted to another thing as "yes" the FlowShopTask
 *   doesn't communicate, between them, the better result. The default
 *   behaviour use communications.
 *
 * - flowshoptask.randominit
 *   activate a random upper bound initialization is setted "yes". This is the
 *   default behaviour.
 *
 * - flowshoptask.progressivedeployement
 *   if is setted "yes", we delegate the virtual node activation and the node
 *   managing to the Manager, which use a node as soon as is possible;
 *   otherwise we wait while deployemnt are not finish.
 *
 */
public class Main {
    public static final Logger logger = ProActiveLogger.getLogger(
            "proactive.examples.flowshop");
    private static final String USAGE = "java " + Main.class.getName() +
        " -bench taillard_bench_file " +
        "-desc xml_descriptor [-lb lower_bound] [-up upper_bound]";

    private static class Args {
        private String taillardBenchFile = null;
        private ArrayList<String> xmlDescriptor = new ArrayList<String>();
        private long lowerBound = -1;
        private long upperBound = -1;
    }

    private static class FSProperties {
        private static boolean com = true;
        private static boolean randominit = true;
        private static boolean taillard = false;

        static {
            fill();
        }

        public static void fill() {
            if (System.getProperty("flowshoptask.com") != null) {
                com = "yes".equals(System.getProperty("flowshoptask.com"));
            }
            if (System.getProperty("flowshoptask.randominit") != null) {
                randominit = "yes".equals(System.getProperty(
                            "flowshoptask.randominit"));
            }
            if (System.getProperty("flowshopparser.taillard") != null) {
                taillard = "yes".equals(System.getProperty(
                            "flowshopparser.taillard"));
                if (taillard) {
                    Main.logger.warn("WE PARSE ORIGINAL TAILLARD FILE");
                }
            }
        }
    }

    /**
     * Print <code>msg</code> in error output.
     * @param msg the message or <code>null</code>.
     */
    private static void usage(String msg) {
        if (msg != null) {
            System.err.println(msg);
        }
        System.err.println("Usage:\n" + USAGE);
        System.exit(1);
    }

    /**
     * Parsing command line arguments.
     * @param args Arguments from command line.
     * @return an <code>Args</code> object with arguments from command line.
     */
    private static Args parseArgs(String[] args) {
        Args parsed = new Args();

        int index = 0;
        while (index < args.length) {
            String argname = args[index];
            if ("-bench".equalsIgnoreCase(argname)) {
                index++;
                if (index >= args.length) {
                    usage(argname);
                }
                parsed.taillardBenchFile = args[index];
                index++;
                continue;
            } else if ("-desc".equalsIgnoreCase(argname)) {
                index++;
                if (index >= args.length) {
                    usage(argname);
                }
                parsed.xmlDescriptor.add(args[index]);
                while ((++index < args.length) &&
                        (!"-ub".equalsIgnoreCase(args[index]) ||
                        !"-lb".equalsIgnoreCase(args[index]))) {
                    parsed.xmlDescriptor.add(args[index]);
                }
                continue;
            } else if ("-lb".equalsIgnoreCase(argname)) {
                index++;
                if (index >= args.length) {
                    usage(argname);
                }
                try {
                    parsed.lowerBound = Long.parseLong(args[index]);
                } catch (NumberFormatException e) {
                    usage("lower_bound must be a number");
                }
                index++;
                continue;
            } else if ("-ub".equalsIgnoreCase(argname)) {
                index++;
                if (index >= args.length) {
                    usage(argname);
                }
                try {
                    parsed.upperBound = Long.parseLong(args[index]);
                } catch (NumberFormatException e) {
                    usage("upper_bound must be a number");
                }
                index++;
                continue;
            }
            usage("Unknow argumnent " + argname);
        }
        return parsed;
    }

    public static void exit(ArrayList<ProActiveDescriptor> pads, int returnCode) {
        try {
            for (Iterator<ProActiveDescriptor> iter = pads.iterator();
                    iter.hasNext();) {
                ProActiveDescriptor pad = iter.next();
                pad.killall(false);
            }
        } catch (ProActiveException e) {
            Main.logger.error("May be not all jvm nodes can be kill.", e);
        }
        System.exit(returnCode);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // Parsing command line
        if ((args.length < 4)) {
            usage("Bad number of arguments (" + args.length +
                ", expected 4 or more)");
        }
        Args parsed = parseArgs(args);

        if (logger.isDebugEnabled()) {
            logger.debug("Command line arguments:");
            logger.debug("Taillard bench file path: " +
                parsed.taillardBenchFile);
            logger.debug(parsed.xmlDescriptor.size() +
                " XML deployment descriptor path: " + parsed.xmlDescriptor);
        }

        // Activate the deployment
        ArrayList<ProActiveDescriptor> pads = new ArrayList<ProActiveDescriptor>();
        ArrayList<VirtualNode> vns = new ArrayList<VirtualNode>();
        try {
            for (Iterator<String> iter = parsed.xmlDescriptor.iterator();
                    iter.hasNext();) {
                String descriptor = iter.next();
                ProActiveDescriptor pad = ProDeployment.getProactiveDescriptor(descriptor);
                pads.add(pad);
                VirtualNode[] currentVNs = pad.getVirtualNodes();
                pad.activateMappings();
                vns.addAll(Arrays.asList(currentVNs));
            }
        } catch (ProActiveException e) {
            logger.fatal("Couldn't deploying nodes", e);
            Main.exit(pads, 1);
        }

        FlowShop fs = null;

        // Parsing taillard file
        try {
            fs = FileParser.parseFile(new File(parsed.taillardBenchFile),
                    FSProperties.taillard);
        } catch (IOException e) {
            Main.logger.fatal("Failed to open file : " +
                parsed.taillardBenchFile + ". Exit!");
            Main.exit(pads, 1);
        } catch (BabFileFormatException e) {
            e.printStackTrace();
            Main.logger.fatal("Unparsable file's format : " +
                parsed.taillardBenchFile + ". Exit!");
            Main.exit(pads, 1);
        }

        Main.logger.info("Communication in FlowShopTask are " +
            ((FSProperties.com) ? "enable" : "disable") +
            "\nRandom initialisation in FlowShopTask are " +
            ((FSProperties.randominit) ? "enable" : "disable"));
        Task task = new FlowShopTask(fs, parsed.lowerBound, parsed.upperBound,
                FSProperties.com, FSProperties.randominit);

        Manager manager = null;

        try {
            if (vns.size() > 1) {
                manager = ProActiveBranchNBound.newBnB(task,
                        vns.toArray(new VirtualNode[vns.size()]),
                        BasicQueueImpl.class.getName());
            } else {
                manager = ProActiveBranchNBound.newBnB(task, vns.get(0),
                        BasicQueueImpl.class.getName());
            }
        } catch (ActiveObjectCreationException e) {
            Main.logger.error("A problem occur while creating the manager.", e);
            return;
        } catch (NodeException e) {
            Main.logger.error("An error occur with a node while creating the manager.",
                e);
            return;
        }

        System.out.println("Start manager");
        long computationTime = System.currentTimeMillis();
        Result result = manager.start();
        System.out.println("Manager started ...");
        ProFuture.waitFor(result);
        computationTime = System.currentTimeMillis() - computationTime;
        System.out.println("The best solution is:\n" + result +
            " and the total time " + computationTime + " " +
            fs.cumulateTimeOnLastMachine);
        exit(pads, 0);
    }
}
