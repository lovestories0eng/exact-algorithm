package methods;

import global.Constants;
import models.Edge.HeterogeneousEdge;
import models.graph.HeterogeneousGraph;
import models.node.HeterogeneousNode;
import models.node.Node;

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
    public static ArrayList<Node> maxFlow(HeterogeneousGraph graph, int startPoint, int endPoint) {
        // 所有节点的状态都标记为false
        graph.reInitNodeset();

        ArrayList<Integer> parent = new ArrayList<>();

        int indexU, indexV;
        int u, v;
        int maxFlow = 0;
        ArrayList<Node> homogeneousNodes = new ArrayList<>();

        while (bfs(graph, startPoint, endPoint, parent)) {

            int pathFlow = 1;
            int endIndex = 0, startIndex = 0;
            // 超过元路径长度的一半时和不超过元路径长度的一半时分别对待
            for (int i = 0; i < graph.nodeSet.size(); i++) {
                if (graph.nodeSet.get(i).id == endPoint)
                    endIndex = i;
                else if (graph.nodeSet.get(i).id == startPoint)
                    startIndex = i;
            }

            Node node = new Node();
            node.id = graph.nodeSet.get(parent.get(endIndex)).id;
            homogeneousNodes.add(node);

            // 记录路径
            ArrayList<Integer> pathRecords = new ArrayList<>();
            // 记录路径方向
            ArrayList<Boolean> pathDirections = new ArrayList<>();
            // 在我们的问题中，每条路径的网络流量一定是1，所以不用找出路径的最小流量。

            // 更新网络流量
            for (indexV = endIndex; indexV != startIndex; indexV = parent.get(indexV)) {
                indexU = parent.get(indexV);
                // 后继节点
                v = graph.nodeSet.get(indexV).id;
                // 前驱节点
                u = graph.nodeSet.get(indexU).id;

                pathRecords.add(indexV);

                int size;
                size = graph.graphHashMap.get(u).size();
                // 遍历邻接链表
                for (int i = 0; i < size; i++) {
                    int tmp = graph.graphHashMap.get(u).get(i).getEndPoint();
                    // 寻找边的后继节点
                    if (tmp == v) {
                        graph.graphHashMap.get(u).get(i).capacity -= pathFlow;
                        pathDirections.add(graph.graphHashMap.get(u).get(i).direction);
                    }
                }

                size = graph.graphHashMap.get(v).size();
                // 遍历邻接链表
                for (int i = 0; i < size; i++) {
                    int tmp = graph.graphHashMap.get(v).get(i).getEndPoint();
                    // 寻找边的前驱节点
                    if (tmp == u) {
                        graph.graphHashMap.get(v).get(i).capacity += pathFlow;
                    }
                }
            }
            pathRecords.add(startIndex);
            // 列表倒置
            Collections.reverse(pathRecords);
            Collections.reverse(pathDirections);
            maxFlow += pathFlow;
            parent = new ArrayList<>();

            // TODO: 由于边不相交和点不相交的对称性，需要把hashMap和hashMapReverse中的值也更新。
            int currentPathLength = 0;
            int totalMetaPathLength = Constants.META_PATH_LENGTH - 1;
            // 与虚拟锚点相连的边不需要管
            for (int i = 0; i < pathDirections.size() - 1; i++) {
                boolean tmpDirection = pathDirections.get(i);
                // 获取起始点的id
                int nodeIdStart = graph.nodeSet.get(pathRecords.get(i)).id;
                // 获取终止点的id
                int nodeIdEnd = graph.nodeSet.get(pathRecords.get(i + 1)).id;
                // 如果是正向前进
                if (tmpDirection) {
                    if (currentPathLength < totalMetaPathLength / 2) {
                        // 遍历起始点的邻接链表
                        for (int j = 0; j < graph.hashMap.get(nodeIdStart).size(); j++) {
                            if (graph.hashMap.get(nodeIdStart).get(j).endPoint == nodeIdEnd) {
                                graph.hashMap.get(nodeIdStart).get(j).capacity -= 1;
                            }
                        }
                    } else if (currentPathLength >= totalMetaPathLength / 2) {
                        // 遍历终止点的邻接链表
                        for (int j = 0; j < graph.hashMapReverse.get(nodeIdStart).size(); j++) {
                            if (graph.hashMapReverse.get(nodeIdStart).get(j).startPoint == nodeIdEnd) {
                                graph.hashMapReverse.get(nodeIdStart).get(j).capacity -= 1;
                            }
                        }
                    }
                    currentPathLength++;
                }
                // 如果是反向前进
                else {
                    if (currentPathLength <= totalMetaPathLength / 2) {
                        // 遍历起始点的邻接链表
                        for (int j = 0; j < graph.hashMap.get(nodeIdEnd).size(); j++) {
                            if (graph.hashMap.get(nodeIdEnd).get(j).endPoint == nodeIdStart) {
                                graph.hashMap.get(nodeIdEnd).get(j).capacity += 1;
                            }
                        }
                    } else if (currentPathLength > totalMetaPathLength / 2) {
                        // 遍历终止点的邻接链表
                        for (int j = 0; j < graph.hashMapReverse.get(nodeIdEnd).size(); j++) {
                            if (graph.hashMapReverse.get(nodeIdEnd).get(j).startPoint == nodeIdStart) {
                                graph.hashMapReverse.get(nodeIdEnd).get(j).capacity += 1;
                            }
                        }
                    }
                    currentPathLength--;

                }
            }
            for (int i = 0; i < pathRecords.size(); i++) {
                System.out.printf("%d ", graph.nodeSet.get(pathRecords.get(i)).id);
            }
            System.out.println();
        }
        System.out.println("从点" + startPoint + "到" + "点" + endPoint + "的最大流为" + maxFlow);
        return homogeneousNodes;
    }

    /**
     * 广度优先搜索判断两点之间有无路径并把路径存储起来
     */
    public static boolean bfs(
            HeterogeneousGraph graph,
            int startPoint,
            int endPoint,
            ArrayList<Integer> parent
    ) {
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
        while (!queue.isEmpty()) {
            int indexU = queue.poll();
            u = graph.nodeSet.get(indexU).id;

            for (int i = 0; i < graph.graphHashMap.get(u).size(); i++) {
                // 遍历点u的邻接链表
                HeterogeneousEdge edge = graph.graphHashMap.get(u).get(i);
                // 得到后继节点
                int point = graph.graphHashMap.get(u).get(i).getEndPoint();
                findLinkNode(parent, visited, queue, u, edge, point);
            }
        }
        return visited.get(endIndex).visited;
    }

    private static void findLinkNode(
            ArrayList<Integer> parent,
            List<HeterogeneousNode> visited,
            Queue<Integer> queue,
            // 前驱节点
            int u,
            HeterogeneousEdge edge,
            // 后继节点
            int point
    ) {

        int indexU = 0;
        for (int i = 0; i < visited.size(); i++) {
            if (visited.get(i).id == u) {
                // 得到前驱节点索引
                indexU = i;
                break;
            }
        }

        int index = 0;
        for (int i = 0; i < visited.size(); i++) {
            if (visited.get(i).id == point) {
                // 得到后继节点索引
                index = i;
                break;
            }
        }
        if (!visited.get(index).visited && edge.capacity > 0) {
            queue.offer(index);
            HeterogeneousNode tmpNode = visited.get(index);
            tmpNode.visited = true;
            visited.set(index, tmpNode);
            parent.set(index, indexU);
        }
    }

    /**
     * 广度优先遍历初步筛选异构图，剔除无法通过元路径与查询节点相连的点。
     * 例如，当元路径为 A->P->T->P->A时，去除无法通过元路径与查询节点相连的点，
     * 同时去除其它类型为 “T”、“V”之类的节点。
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
                    if ((Objects.equals(nodeSet.get(point).nodeType, Constants.META_PATH[currentPathLength]) || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
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

                        if ((Objects.equals(nodeSet.get(point).nodeType, Constants.META_PATH[currentPathLength]) || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
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
