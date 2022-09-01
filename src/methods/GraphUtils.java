package methods;

import global.Constants;
import models.Edge.HeterogeneousEdge;
import models.graph.HeterogeneousGraph;
import models.node.HeterogeneousNode;

import java.util.*;

/**
 * TODO: 修改邻接链表的数据结构，把其中的元素从点转换为边
 * TODO: 修改邻接链表的存储结构，使其从原来的数组变成HashMap
 **/

public class GraphUtils {
    /**
     * 利用最大流找出异构图节点所有的元路径
     * 返回与起始节点想连的点，用于同构图的构建
     * 在我们要解决的问题中，
     * 网络流代表的是边的共享次数，
     * 所以每次边中边的流量只能减一
     **/
    public static void maxFlow(HeterogeneousGraph graph, int startPoint, int endPoint) {
        // 所有节点的状态都标记为false
        graph.reInitNodeset();

        ArrayList<Integer> parent = new ArrayList<>();

        int indexU, indexV;
        int u, v;
        int maxFlow = 0;

        while (bfs(graph, startPoint, endPoint, parent)) {
            int pathFlow = Integer.MAX_VALUE;
            // pathFlow = 1;
            int endIndex = 0, startIndex = 0;
            // 超过元路径长度的一半时和不超过元路径长度的一半时分别对待
            for (int i = 0; i < graph.nodeSet.size(); i++) {
                if (graph.nodeSet.get(i).id == endPoint)
                    endIndex = i;
                else if (graph.nodeSet.get(i).id == startPoint)
                    startIndex = i;
            }
            int pathLength = 0;
            for (indexV = endIndex; indexV != startIndex; indexV = parent.get(indexV)) {
                indexU = parent.get(indexV);
                u = graph.nodeSet.get(indexU).id;

                if (pathLength < (Constants.META_PATH_LENGTH - 1) / 2) {
                    int size = graph.hashMapReverse.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        if (graph.hashMapReverse.get(u).get(i).getStartPoint() == graph.nodeSet.get(indexV).id) {
                            pathFlow = Math.min(pathFlow, graph.hashMapReverse.get(u).get(i).capacity);
                        }
                    }
                } else {
                    int size = graph.hashMap.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        if (graph.hashMap.get(u).get(i).getEndPoint() == graph.nodeSet.get(indexV).id) {
                            pathFlow = Math.min(pathFlow, graph.hashMap.get(u).get(i).capacity);
                        }
                    }
                }

                pathLength++;
            }

            // 更新网络流量
            pathLength = 0;
            for (indexV = endIndex; indexV != startIndex; indexV = parent.get(indexV)) {
                indexU = parent.get(indexV);
                v = graph.nodeSet.get(indexV).id;
                u = graph.nodeSet.get(indexU).id;
                if (pathLength < (Constants.META_PATH_LENGTH - 1) / 2) {
                    int size = graph.hashMapReverse.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        int tmp = graph.hashMapReverse.get(u).get(i).getStartPoint();
                        // 寻找到反向邻接链表中边的起始点
                        if (tmp == v) {
                            graph.hashMapReverse.get(u).get(i).capacity -= pathFlow;
                            // graph.hashMapReverse.get(u).get(i).capacity -= 1;
                        }
                    }
                } else {
                    int size = graph.hashMap.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        int tmp = graph.hashMap.get(u).get(i).getEndPoint();
                        // 寻找到邻接链表中边的终止点
                        if (tmp == v) {
                            graph.hashMap.get(u).get(i).capacity -= pathFlow;
                            // graph.hashMap.get(u).get(i).capacity -= 1;
                        }
                    }
                }
                pathLength++;
            }
            // maxFlow += pathFlow;
            maxFlow += 1;
        }
        System.out.println("从点" + startPoint + "到" + "点" + endPoint + "的最大流为" + maxFlow);
    }

    /**
     * 广度优先搜索判断两点之间有无路径并把路径存储起来
     *
     */
    public static boolean bfs(HeterogeneousGraph graph, int startPoint, int endPoint, ArrayList<Integer> parent) {
        // visited数组用来存储索引
        List<HeterogeneousNode> visited = new ArrayList<>(graph.getNodeSet());
        HeterogeneousNode tmpNode;
        for (int i = 0; i < graph.vertexNum; i++) {
            // 全部初始化为false
            tmpNode = visited.get(i);
            tmpNode.visited = false;
            visited.set(i, tmpNode);
        }

        // parent数组用来存储搜索出来的路径，以索引的形式记录
        for (int i = 0; i < graph.vertexNum; i++) {
            parent.add(-100);
        }
        Queue<Integer> queue = new LinkedList<>();
        int currentPathLength = 0;
        int halfMetaPath = Constants.META_PATH_LENGTH / 2;


        int startIndex = 0, endIndex = 0;
        // 找到起始点在nodeSet中的索引
        for (int i = 0; i < graph.nodeSet.size(); i++) {
            if (graph.nodeSet.get(i).id == startPoint) {
                startIndex = i;
            }
            if (graph.nodeSet.get(i).id == endPoint) {
                endIndex = i;
            }
        }
        parent.set(startIndex, -1);
        queue.offer(startIndex);

        tmpNode = visited.get(startIndex);
        tmpNode.visited = true;
        visited.set(startIndex, tmpNode);
        int u;
        // 初始化节点类型为空
        String tmpType = "";
        List<HeterogeneousNode> nodeSet = graph.getNodeSet();
        while (!queue.isEmpty()) {
            int indexU = queue.poll();
            u = graph.nodeSet.get(indexU).id;
            if (!tmpType.equals(nodeSet.get(indexU).nodeType)) {
                currentPathLength++;
                tmpType = nodeSet.get(indexU).nodeType;
            }
            // 未超过元路径长度的一半时
            if (currentPathLength <= halfMetaPath) {
                for (int i = 0; i < graph.hashMap.get(u).size(); i++) {
                    // 遍历点u的邻接链表
                    HeterogeneousEdge edge = graph.hashMap.get(u).get(i);
                    // 得到后继节点
                    int point = graph.hashMap.get(u).get(i).getEndPoint();
                    findLinkNode(parent, visited, queue, currentPathLength, u, nodeSet, edge, point);
                }
            }
            // 超过元路径长度的一半时
            else {
                // 遍历到目标节点的时候会产生null值
                if (graph.hashMapReverse.get(u) != null) {
                    for (int i = 0; i < graph.hashMapReverse.get(u).size(); i++) {
                        // 遍历点u的反向邻接链表
                        HeterogeneousEdge edge = graph.hashMapReverse.get(u).get(i);
                        // 得到后继节点
                        int point = graph.hashMapReverse.get(u).get(i).getStartPoint();
                        System.out.println(point + " " + graph.hashMapReverse.get(u).get(i).getEndPoint() + " " + graph.hashMapReverse.get(u).get(i).capacity);
                        findLinkNode(parent, visited, queue, currentPathLength, u, nodeSet, edge, point);
                    }
                }
            }
        }
        return visited.get(endIndex).visited;
    }

    private static void findLinkNode(
            ArrayList<Integer> parent,
            List<HeterogeneousNode> visited,
            Queue<Integer> queue,
            int currentPathLength,
            // 前驱节点
            int u,
            List<HeterogeneousNode> nodeSet,
            HeterogeneousEdge edge,
            // 后继节点
            int point
    ) {
        int index = 0;
        for (int i = 0;i < visited.size(); i++) {
            if (visited.get(i).id == point) {
                // 得到后继节点索引
                index = i;
                break;
            }
        }

        int indexU = 0;
        for (int i = 0;i < visited.size(); i++) {
            if (visited.get(i).id == u) {
                // 得到前驱节点索引
                indexU = i;
                break;
            }
        }

        System.out.println(nodeSet.get(indexU).nodeType);
        System.out.println(Constants.META_PATH[currentPathLength]);
        if ((Objects.equals(nodeSet.get(indexU).nodeType, Constants.META_PATH[currentPathLength])
                // 虚拟节点
                || Objects.equals(nodeSet.get(indexU).nodeType, "virtual"))
                // 判断后继节点是否被访问过
                && !visited.get(index).visited
                // 判断前驱节点和后继节点的连边容量是否大于零
                && edge.capacity > 0
        ) {
            queue.offer(index);
            HeterogeneousNode tmpNode = visited.get(index);
            tmpNode.visited = true;
            visited.set(index, tmpNode);
            parent.set(index, indexU);
        }
    }

    /**
     * 广度优先遍历初步筛选异构图，剔除无法通过元路径与查询节点相连的点
     **/
    public static List<HeterogeneousNode> bfsTraverse(HeterogeneousGraph graph, int queryNodeId) {
        List<HeterogeneousNode> nodes = new ArrayList<>();

        ArrayList<Boolean> visited = new ArrayList<>();
        for (int i = 0; i < graph.vertexNum; i++) {
            // 全部初始化为false
            visited.add(false);
        }
        Queue<Integer> queue = new LinkedList<>();
        int currentPathLength = 0;
        int halfMetaPath = Constants.META_PATH_LENGTH / 2;

        queue.offer(queryNodeId);
        visited.set(queryNodeId, true);
        int u;
        // 初始化节点类型为空
        String tmpType = "";
        List<HeterogeneousNode> nodeSet = graph.getNodeSet();
        while (!queue.isEmpty()) {
            u = queue.poll();
            // 存储与查询节点有关的点
            String nodeType = "";
            for (HeterogeneousNode heterogeneousNode : nodeSet) {
                if (heterogeneousNode.id == u) {
                    nodeType = heterogeneousNode.nodeType;
                    nodes.add(heterogeneousNode);
                }
            }
            if (!tmpType.equals(nodeType)) {
                currentPathLength++;
                tmpType = nodeSet.get(u).nodeType;
                if (currentPathLength >= Constants.META_PATH_LENGTH + 1) {
                    break;
                }
            }
            // 未超过元路径长度的一半时
            if (currentPathLength <= halfMetaPath) {
                for (int i = 0; i < graph.hashMap.get(u).size(); i++) {
                    int point = graph.hashMap.get(u).get(i).getEndPoint();
                    if ((Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
                            && !visited.get(point)
                            ) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
            // 超过元路径长度的一半时
            else {
                // 遍历到目标节点的时候会产生null值
                if (graph.hashMapReverse.get(u) != null) {
                    for (int i = 0; i < graph.hashMapReverse.get(u).size(); i++) {
                        int point = graph.hashMapReverse.get(u).get(i).getStartPoint();

                        if ((Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
                                && !visited.get(point)
                                ) {
                            queue.offer(point);
                            visited.set(point, true);
                        }
                    }
                }
            }
        }
        return nodes;
    }
}
