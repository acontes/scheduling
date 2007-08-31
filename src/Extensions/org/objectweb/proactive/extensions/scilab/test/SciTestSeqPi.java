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
package org.objectweb.proactive.extensions.scilab.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import javasci.SciData;
import javasci.Scilab;

import org.objectweb.proactive.extensions.scilab.util.SciMath;


class SciTestSeqPi {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Invalid number of parameter : " + args.length);
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        PrintWriter writer = new PrintWriter(new BufferedWriter(
                    new FileWriter(args[1])));

        int precision;
        String line;
        double startTime;
        double endTime;

        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("#")) {
                continue;
            }

            if (line.trim().equals("")) {
                break;
            }

            precision = Integer.parseInt(line);
            startTime = System.currentTimeMillis();
            Scilab.Exec(SciMath.formulaPi("pi", 0, precision));
            SciData sciPi = (SciData) Scilab.receiveDataByName("pi");
            endTime = System.currentTimeMillis();

            System.out.println(sciPi);
            writer.println(precision + " " + (endTime - startTime));
        }

        reader.close();
        writer.close();
    }
}
