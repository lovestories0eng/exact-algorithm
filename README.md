### Data Structure
**vertex.txt: vertex id --- vertex type** \
**edge.txt: edge id --- edge type** \
**graph.txt: vertex id --- list of neighbor vertex id and edge id**

The vertex types and edge types are numbered as follows:

vertex type \
Paper : 0; \
Author : 1; \
Venue : 2; \
Topic : 3; 

edge type \
Paper->Author 0; \
Paper->Venue 1;  \
Paper->Topic 2; \
Author->Paper : 3; \
Venue->Paper : 4; \
Topic->Paper : 5; 

### TODO
* 把吴越学长的Truss Decomposition移植为一个函数，输入为邻接链表结构的图与查询节点、输出为包含查询节点的最大k-truss。
* 学习方一向的代码，先利用网络最大流并结合边的共享次数构建同构图，再对同构图进行Truss Decomposition。
* 固定k不变，观察图的边数（点数）随共享次数的变化。
* 固定共享次数，观察能够找到的最大k-truss值随共享次数的变化。
* 完成从查询节点构建初始图的算法。