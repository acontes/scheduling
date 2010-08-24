importPackage(java.io);


var inFile = new File(args[0] + "/input");
var outFile = new File(args[0] + "/output");

if (inFile.exists() && outFile.exists()) {
	var input = new BufferedReader(new FileReader(inFile));
	var output = new BufferedReader(new FileReader(outFile));

	var inLines = 0;
	var outLines = 0;

	while (input.readLine() != null) {
		inLines ++;
	}
	while (output.readLine() != null) {
		outLines ++;
	}
	input.close();
	output.close();

	if (inLines > outLines) {
		loop = true;
	} else {
		loop = false;
	}

	input.close();
}
