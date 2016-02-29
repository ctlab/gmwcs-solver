# gmwcs-solver
Compilation
===========

Get source from github:

    git clone <HTTPS clone URL (see on the right side of this page)>
    
Then you should install concert library of CPLEX.
It's located in "cplex/lib" directory from CPLEX STUDIO root path.
For example, 

    mvn install:install-file -Dfile=/opt/ibm/ILOG/CPLEX_Studio1251/cplex/lib/cplex.jar -DgroupId=com.ibm -DartifactId=cplex -Dversion=12.5.1 -Dpackaging=jar
    
After that you can build the project using maven:

    mvn install
    
And jar file with name "gmwcs.jar" will appear in "target" directory
    
Running
=======

To run program you should set jvm parameter java.library.path to directory of CPLEX binaries like that:

    java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio1251/cplex/bin/x86-64_sles10_4.1/ -jar gmwcs.jar

See more help by using flag -h.

Problem
=========

Solver solves Maximum Weighted Connected Subgraph problem with weighted vertices and edges. 

Sample
=========

Node file(node_name  node_weight):

    1   -3.0
    2   -5.0
    3   0.0
    4   2.0
    5   1.0

Edge file(edge_from edge_to edge_weight):

    1   2   4.0
    1   3   7.0
    2   3   5.0
    3   4   1.0
    4   5   -2.0
    1   5   -1.5

![Sample](/sample.png?raw=true "Sample")

Red units in graph below - solution.

![Sample](/sample_solved.png?raw=true "Solution")

Running sample
==============

    java -Djava.library.path=PATH_TO_CPLEX -jar signal.jar -n nodes -e edges
