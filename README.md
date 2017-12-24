# gmwcs-solver
Compilation
===========

Get source from github:

    git clone <HTTPS clone URL (see on the right side of this page)>
    
Then you should install concert library of CPLEX.
It's located in "cplex/lib" directory from CPLEX STUDIO root path.
For example, 

    mvn install:install-file -Dfile=/opt/ibm/ILOG/CPLEX_Studio1263/cplex/lib/cplex.jar -DgroupId=com.ibm -DartifactId=cplex -Dversion=12.6.3 -Dpackaging=jar
    
After that you can build the project using maven:

    mvn install
    
And jar file with name "gmwcs.jar" will appear in "target" directory
    
Running
=======

To run program you should set jvm parameter java.library.path to directory of CPLEX binaries like that:

    java -Xss4M -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio1263/cplex/bin/x86-64_sles10_4.1/ -jar gmwcs.jar

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

    java -Xss4M -Djava.library.path=PATH_TO_CPLEX -jar target/gmwcs.jar -n nodes -e edges
    
The output should like this:
```
Graph decomposing takes 0.011 seconds.
Warning: Control callbacks may disable some MIP features.
Generated priority order by increasing cost per coefficient count.
Found incumbent of value 0,000000 after 0,00 sec. (0,00 ticks)

Root node processing (before b&c):
  Real time             =    0,00 sec. (0,00 ticks)
Sequential b&c:
  Real time             =    0,00 sec. (0,00 ticks)
                          ------------
Total (root+branch&cut) =    0,00 sec. (0,00 ticks)
Generated priority order by increasing cost per coefficient count.
Tried aggregator 2 times.
MIP Presolve eliminated 6 rows and 0 columns.
MIP Presolve modified 29 coefficients.
Aggregator did 4 substitutions.
Reduced MIP has 66 rows, 30 columns, and 196 nonzeros.
Reduced MIP has 24 binaries, 0 generals, 0 SOSs, and 0 indicators.
Presolve time = 0,00 sec. (0,23 ticks)
Found incumbent of value 3,000000 after 0,00 sec. (0,42 ticks)
Probing fixed 5 vars, tightened 1 bounds.
Probing time = 0,00 sec. (0,10 ticks)
Cover probing fixed 0 vars, tightened 1 bounds.
Tried aggregator 1 time.
MIP Presolve eliminated 18 rows and 9 columns.
MIP Presolve modified 8 coefficients.
Reduced MIP has 48 rows, 21 columns, and 140 nonzeros.
Reduced MIP has 16 binaries, 0 generals, 0 SOSs, and 0 indicators.
Presolve time = 0,00 sec. (0,16 ticks)
Probing time = 0,00 sec. (0,05 ticks)
Cover probing fixed 0 vars, tightened 1 bounds.
Tried aggregator 1 time.
MIP Presolve modified 7 coefficients.
Reduced MIP has 48 rows, 21 columns, and 140 nonzeros.
Reduced MIP has 16 binaries, 0 generals, 0 SOSs, and 0 indicators.
Presolve time = 0,00 sec. (0,08 ticks)
Probing time = 0,00 sec. (0,05 ticks)
Clique table members: 33.
MIP emphasis: balance optimality and feasibility.
MIP search method: dynamic search.
Parallel mode: none, using 1 thread.
Root relaxation solution time = 0,00 sec. (0,05 ticks)

        Nodes                                         Cuts/
   Node  Left     Objective  IInf  Best Integer    Best Bound    ItCnt     Gap

*     0+    0                            3,0000       12,0000           300,00%
      0     0       11,0000     6        3,0000       11,0000        5  266,67%
*     0+    0                           11,0000       11,0000             0,00%
      0     0        cutoff             11,0000       11,0000        5    0,00%
Elapsed time = 0,01 sec. (1,09 ticks, tree = 0,01 MB, solutions = 2)

Root node processing (before b&c):
  Real time             =    0,01 sec. (1,09 ticks)
Sequential b&c:
  Real time             =    0,00 sec. (0,00 ticks)
                          ------------
Total (root+branch&cut) =    0,01 sec. (1,09 ticks)
Infeasibility row 'c1':  0  = 1.
Presolve time = 0,00 sec. (0,00 ticks
```

Two files with the result `nodes.out` and `edges.out` will be created:
```
$ cat nodes.out 
1	-3.0
2	-5.0
3	0.0
4	2.0
5	n/a
#subnet node score	-6.0
$ cat edges.out
1	2	4.0
1	3	7.0
2	3	5.0
3	4	1.0
4	5	n/a
1	5	n/a
#subnet edge score	17.0
```
