package vertexDisjoint;

import common.BatchLinker;
import global.Config;
import models.MetaPath;
import utils.InitialGraphConstructor;

import java.util.*;

// 从查询点扩展构建初始图
public class QueryNodeExpandStrategyDivided implements InitialGraphConstructor {
    private final int[][] graph;//data graph, including vertex IDs, edge IDs, and their link relationships
    private final int[] vertexType;//vertex -> type
    private final int[] edgeType;//edge -> type
    private final int[] edgeUsedTimes;//edge -> used times
    private final HashMap<Integer, Set<Integer>> homoGraph = new HashMap<>();
    // 记录找到的所有节点
    Set<Integer> vertexFound = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>> pathRecordMap = new HashMap<>();
    Set<Integer> keepSet = null;
    HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge;
    // path conflict
    HashMap<ArrayList<Integer>, Double> pathMapConflict;
    // <vertex pair -> path with lowest conflict>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<Integer>> vertexPairMapPath;
    // <vertex pair -> path conflict>
    HashMap<Map.Entry<Integer, Integer>, Double> vertexPairMapConflict;
    // TODO: 为每个点对存储所有的路径，从而在知道可能受影响的点对之后更新其冲突值与对应路径
    // <vertex pair -> all path between two vertex>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<ArrayList<Integer>>> vertexPairMapAllPath;
    // 存储所有路径
    ArrayList<ArrayList<Integer>> allPaths = new ArrayList<>();
    // <edge id -> ArrayList<path>>
    HashMap<Integer, ArrayList<ArrayList<Integer>>> edgeMapPath;
    // 存储通过一跳搜索找到的新的节点
    Set<Integer> newFoundVertex = new HashSet<>();
    // 存储上一次一跳搜索找到的点
    Set<Integer> foundVertex = new HashSet<>();
    int pathLen = 0;
    double globalVertexPairConflict;
    double globalPathConflict;
    // 存储冲突度最小的点对集合
    HashMap<Double, ArrayList<ArrayList<Integer>>> conflictMapVertexPairPath;
    // 存储vertexPair的大小
    ArrayList<Integer> vertexPairRecorder = new ArrayList<>();

    public QueryNodeExpandStrategyDivided(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.vertexPairMapEdge = vertexPairMapEdge;
    }

    public int[][] query(int queryId, MetaPath metaPath) {
        long startTime = System.currentTimeMillis();

        this.pathLen = metaPath.pathLen + 1;
        if (metaPath.vertex[0] != vertexType[queryId]) {
            System.out.println("查询节点类型与元路径类型不匹配！");
            return null;
        }

        // 存储寻找到的所有节点
        vertexFound.add(queryId);
        foundVertex.add(queryId);

        // step 1: compute the connected subgraph via batch-search with labeling (BSL)
        BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
        keepSet = batchLinker.link(queryId, metaPath);

        //step 2: perform pruning
        // 根据路径的第一条边删除了一遍出度<k的节点
        // SECONDVertexTYPE: P, SECONDEdgeTYPE: A -> P
        int SECONDVertexTYPE = metaPath.vertex[1], SECONDEdgeTYPE = metaPath.edge[0];
        Iterator<Integer> keepIter = keepSet.iterator();
        while (keepIter.hasNext()) {
            int id = keepIter.next();
            int count = 0;
            // 遍历所有与查询节点p邻居的点
            for (int i = 0; i < graph[id].length; i += 2) {
                int nbVId = graph[id][i], nbEId = graph[id][i + 1];
                if (vertexType[nbVId] == SECONDVertexTYPE && edgeType[nbEId] == SECONDEdgeTYPE) {
                    count++;
                    if (count >= Config.k - 1) break;
                }
            }
            if (count < Config.k - 1) keepIter.remove();
        }
        if (!keepSet.contains(queryId)) return null;

        while (true) {
            // step 3: expand the graph from the new-found node, find possibly linked vertex
            newFoundVertex = this.oneHopTraverse(foundVertex, metaPath, true);
            // 如果找不到新的点则结束
            if (newFoundVertex.size() == 0) break;
            // 根据树结构得到所有路径
            createPathSet(foundVertex, pathRecordMap);
            // step 4: link edges between new-found vertex set and original vertex set according to the pair conflict rule
            initPathMapConflictAndConflictMapVertexPairPath();
            // link edges with the lowest conflict utils vertex pairs are linked or no edges exists
            foundVertex = new HashSet<>();
            traverseVertexPair();
            // step 5: link edges inner the new-found vertex
            oneHopTraverse(foundVertex, metaPath, false);
            createPathSet(foundVertex, pathRecordMap);
            initPathMapConflictAndConflictMapVertexPairPath();
            traverseVertexPair();
            System.out.println("Break point!");
        }
        long endTime = System.currentTimeMillis();
        System.out.println("运行时间：" + (endTime - startTime) + "ms");

        return null;
    }

    private void initPathMapConflictAndConflictMapVertexPairPath() {
        pathMapConflict = new HashMap<>();
        conflictMapVertexPairPath = new HashMap<>();
        for (ArrayList<Integer> tmpPath : allPaths) {
            double conflict = calcPathConflict(tmpPath);
            if (conflict != -1) {
                pathMapConflict.put(tmpPath, conflict);
                ArrayList<ArrayList<Integer>> conflictMapPath = conflictMapVertexPairPath.get(conflict);
                if (conflictMapPath == null) {
                    conflictMapPath = new ArrayList<>();
                    conflictMapVertexPairPath.put(conflict, conflictMapPath);
                }
                conflictMapPath.add(tmpPath);
            }
        }

        this.initVertexPairMap();
    }

    private void traverseVertexPair() {
        vertexPairRecorder.add(vertexPairMapConflict.size());
        System.out.println(vertexPairMapConflict.size());
        // 为什么有时候vertexPairMapConflict大小不变
        while (vertexPairMapConflict.size() != 0) {
            double minConflict = Double.POSITIVE_INFINITY;
            // TODO: 把点对按冲突度进行分组，实现O(1)时间内找到冲突度最小的点对
            // TODO: 如果vertexPair已连边则跳过
            // TODO: 两种方法遍历次数进行比较
            ArrayList<Integer> path = null;
            int count = 0;
            while (path == null) {
                count = 0;
                for (Map.Entry<Double, ArrayList<ArrayList<Integer>>> entry : conflictMapVertexPairPath.entrySet()) {
                    if (entry.getValue().size() == 0) {
                        continue;
                    }
                    if (entry.getKey() < minConflict && entry.getKey() != -1) {
                        minConflict = entry.getKey();
                    }
                }
                ArrayList<ArrayList<Integer>> tmpPathSet = conflictMapVertexPairPath.get(minConflict);
                for (int i = 0; i < tmpPathSet.size(); i++) {
                    count++;
                    ArrayList<Integer> tmpPath = tmpPathSet.get(i);
                    int k = tmpPath.get(0);
                    int v = tmpPath.get(tmpPath.size() - 1);
                    if (vertexPairMapConflict.containsKey(Map.entry(k, v))) {
                        path = tmpPath;
                        break;
                    } else {
                        tmpPathSet.remove(i);
                        i--;
                    }
                }
            }

            // Map.Entry<Integer, Integer> tmp = null;
            // int count = 0;
            // for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
            //     if (entry.getValue() < minConflict) {
            //         tmp = entry.getKey();
            //         minConflict = entry.getValue();
            //     }
            //     count++;
            //     if (count == vertexPairMapConflict.size() && !(minConflict - globalVertexPairConflict < 1e-4)) {
            //         globalVertexPairConflict = minConflict;
            //     }
            //     // 如果已经找到了全局最小冲突值则跳出循环
            //     if (minConflict == globalVertexPairConflict) break;
            // }
            // ArrayList<Integer> path = vertexPairMapPath.get(tmp);

            System.out.println(count);

            // 把发现的点加入”已找到点“集合中
            vertexFound.add(path.get(path.size() - 1));
            foundVertex.add(path.get(path.size() - 1));
            homeGraphBuilder(path);
            // update path conflict and vertex pair conflict
            updateVertexPairMap(path);
        }
    }

    private void initVertexPairMap() {
        vertexPairMapConflict = new HashMap<>();
        vertexPairMapPath = new HashMap<>();
        globalVertexPairConflict = Double.POSITIVE_INFINITY;

        vertexPairMapAllPath = new HashMap<>();
        globalPathConflict = Double.POSITIVE_INFINITY;

        for (Map.Entry<ArrayList<Integer>, Double> entry : pathMapConflict.entrySet()) {
            ArrayList<Integer> key = entry.getKey();
            double conflict = entry.getValue();
            if (conflict < globalPathConflict) {
                globalPathConflict = conflict;
            }
            int k = key.get(0);
            int v = key.get(key.size() - 1);
            // 判断边容量是否大于0
            boolean flag = true;
            for (int i = 0; i < key.size() - 1; i++) {
                int tmpK = key.get(i), tmpV = key.get(i + 1);
                if (edgeUsedTimes[vertexPairMapEdge.get(Map.entry(tmpK, tmpV))] <= 0) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // 记录点对间的所有路径
                if (vertexPairMapAllPath.containsKey(Map.entry(k, v))) {
                    ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(Map.entry(k, v));
                    tmp.add(key);
                } else {
                    ArrayList<ArrayList<Integer>> tmp = new ArrayList<>();
                    tmp.add(key);
                    vertexPairMapAllPath.put(Map.entry(k, v), tmp);
                }
                // 记录点对间的最小冲突值
                if (vertexPairMapConflict.containsKey(Map.entry(k, v))) {
                    if (conflict < vertexPairMapConflict.get(Map.entry(k, v))) {
                        vertexPairMapConflict.put(Map.entry(k, v), conflict);
                        vertexPairMapPath.put(Map.entry(k, v), key);
                    }
                } else {
                    vertexPairMapConflict.put(Map.entry(k, v), conflict);
                    vertexPairMapPath.put(Map.entry(k, v), key);
                }
                if (conflict < globalVertexPairConflict) {
                    globalVertexPairConflict = conflict;
                }
            }
        }
    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    // TODO: optimization -> do not judge whether homoGraph contains some key, just get it, if it's null, then create a new set --- Done
    private void homeGraphBuilder(ArrayList<Integer> path) {
        Set<Integer> set = homoGraph.get(path.get(0));
        if (set != null) {
            set.add(path.get(path.size() - 1));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(path.size() - 1));
            homoGraph.put(path.get(0), newSet);
        }

        set = homoGraph.get(path.get(path.size() - 1));
        if (set != null) {
            set.add(path.get(0));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(0));
            homoGraph.put(path.get(path.size() - 1), newSet);
        }

        // 更新边容量
        for (int i = 0; i < path.size() - 1; i++) {
            edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))]--;
        }

        // 如果点对已连接则去除其在HashMap中的键值对
        int k = path.get(0);
        int v = path.get(path.size() - 1);
        vertexPairMapConflict.remove(Map.entry(k, v));
        vertexPairMapPath.remove(Map.entry(k, v));
        vertexPairMapAllPath.remove(Map.entry(k, v));
        vertexPairMapConflict.remove(Map.entry(v, k));
        vertexPairMapPath.remove(Map.entry(v, k));
        vertexPairMapAllPath.remove(Map.entry(v, k));
    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    private void updateVertexPairMap(ArrayList<Integer> path) {
        // TODO: 使用edgeId或者vertexPair寻找到受影响的路径和点对以此提高速度 --- Done
        for (int i = 0; i < path.size() - 1; i++) {
            int k = path.get(i);
            int v = path.get(i + 1);
            int edgeId = vertexPairMapEdge.get(Map.entry(k, v));
            ArrayList<ArrayList<Integer>> tmpVar = edgeMapPath.get(edgeId);
            for (int j = 0; j < tmpVar.size(); j++) {
                // TODO: 如果此条路径所连接的点对已经无了则无需更新路径，并且可以删掉路径
                ArrayList<Integer> integers = tmpVar.get(j);
                // 更新边冲突
                double conflict = calcPathConflict(integers);
                int vertexPairStart = integers.get(0);
                int vertexPairEnd = integers.get(integers.size() - 1);
                Map.Entry<Integer, Integer> vertexPair = Map.entry(vertexPairStart, vertexPairEnd);
                // Map.Entry<Integer, Integer> vertexPairReverse = Map.entry(vertexPairEnd, vertexPairStart);
                // 如果点对在同构图中已连接则跳过
                if (!vertexPairMapConflict.containsKey(vertexPair))
                    continue;
                if (pathMapConflict.containsKey(integers)) {
                    conflictMapVertexPairPath.get(pathMapConflict.get(integers)).remove(integers);
                    pathMapConflict.put(integers, conflict);
                    ArrayList<ArrayList<Integer>> conflictMap = conflictMapVertexPairPath.get(conflict);
                    ArrayList<ArrayList<Integer>> tmp;
                    if (conflictMap == null) {
                        tmp = new ArrayList<>();
                        conflictMapVertexPairPath.put(conflict, tmp);
                    } else {
                        tmp = conflictMapVertexPairPath.get(conflict);
                    }
                    tmp.add(integers);
                }
                // 更新点对冲突
                // double oldConflict = vertexPairMapConflict.get(vertexPair);
                double newConflict = conflict;
                if (newConflict == -1) {
                    ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(vertexPair);
                    tmp.remove(integers);
                    tmpVar.remove(integers);
                    // 防止索引越界
                    j--;
                    // 如果此点对之间已无路径了则删除点对的键值，直接进行下一轮循环
                    // TODO: 更多的remove
                    if (tmp.size() == 0) {
                        vertexPairMapConflict.remove(vertexPair);
                        vertexPairMapPath.remove(vertexPair);
                        vertexPairMapAllPath.remove(vertexPair);
                        // TODO: a -> b无路径不代表 b -> a 无路径
                        // vertexPairMapConflict.remove(vertexPairReverse);
                        // vertexPairMapPath.remove(vertexPairReverse);
                        // vertexPairMapAllPath.remove(vertexPairReverse);
                        continue;
                    }
                    // 如果恰巧这条路径就是当前vertexMapPath中的路径
                    if (integers == vertexPairMapPath.get(vertexPair)) {
                        Double tmpConflict = Double.POSITIVE_INFINITY;
                        // 另外找出一条冲突最小的路径
                        for (ArrayList<Integer> minPath : tmp) {
                            Double anotherConflict = pathMapConflict.get(minPath);
                            if (anotherConflict < tmpConflict) {
                                vertexPairMapConflict.put(vertexPair, anotherConflict);
                                vertexPairMapPath.put(vertexPair, minPath);
                            }
                        }
                    }
                }
            }
        }
    }

    private double calcPathConflict(ArrayList<Integer> path) {
        double totalTimes = Config.SHARED_TIMES;
        double usedTimesInTotal = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            usedTimesInTotal += totalTimes - edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))];
            // 如果有边的容量小于等于0
            if (edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))] <= 0) {
                // -1代表此条路径已经不可用
                return -1;
            }
        }
        return usedTimesInTotal / (totalTimes * (path.size() - 1));
    }

    /**
     * flag: to indicate whether we find edges from unknown vertex or known vertex
     **/
    private Set<Integer> oneHopTraverse(Set<Integer> startSet, MetaPath metaPath, boolean flag) {
        // TODO: Optimization need, try to filter some vertex
        int pathLen = metaPath.pathLen;
        Set<Integer> batchSet = startSet;
        pathRecordMap = new HashMap<>();
        allPaths = new ArrayList<>();
        for (int index = 0; index < pathLen; index++) {
            int startPoint = 0;
            int targetVType = metaPath.vertex[index + 1], targetEType = metaPath.edge[index];
            Set<Integer> nextBatchSet = new HashSet<>();
            for (int anchorId : batchSet) {
                int[] nbArr = graph[anchorId];
                for (int i = 0; i < nbArr.length; i += 2) {
                    int nbVertexId = nbArr[i], nbEdgeId = nbArr[i + 1];
                    if (targetVType == vertexType[nbVertexId]
                            && targetEType == edgeType[nbEdgeId]
                            && edgeUsedTimes[nbEdgeId] > 0
                    ) {
                        // 记录起始点位置
                        if (index == 0) {
                            startPoint = anchorId;
                        }
                        if (index == metaPath.pathLen - 1) {
                            if (flag) {
                                // 目的是为了找到没有找到过的点，因此如果发现此点已经被找到则跳过
                                if (vertexFound.contains(nbVertexId))
                                    continue;
                            } else {
                                // 只寻找在已知图中的边且必须是新找到的点集合中的
                                if (!foundVertex.contains(nbVertexId) || nbVertexId == startPoint) {
                                    continue;
                                }
                                // 如果同构图中已经有边则跳过
                                Set<Integer> vertexList = homoGraph.get(nbVertexId);
                                if (vertexList.contains(anchorId)) {
                                    continue;
                                }
                            }
                            if (keepSet.contains(nbVertexId)) {
                                nextBatchSet.add(nbVertexId);
                                recordPath(pathRecordMap, anchorId, nbVertexId);
                            }
                        } else {
                            nextBatchSet.add(nbVertexId);
                            recordPath(pathRecordMap, anchorId, nbVertexId);
                        }
                    }
                }
            }
            batchSet = nextBatchSet;
        }
        return batchSet;
    }

    private void createPathSet(Set<Integer> vertexSet, HashMap<Integer, ArrayList<Integer>> pathRecordMap) {
        // TODO: 减少生成路径集合的大小
        // 重新初始化所有路径集合
        allPaths = new ArrayList<>();
        // TODO: 选择边的Id来记录受影响的路径还是选择vertexPair来记录受影响的路径 --- Done
        // 选择edgeId: 需要先利用边获得点对，然后映射到edgeId以获得边的值，这样选择忽视了vertexPairMapEdge中的信息
        // 选择vertexPair，直接利用vertexPairMapEdge中的信息，但多了一步从vertexPair映射到edgeId的步骤
        // 先选择edgeId，关注时间
        // 重新初始化边对应的受影响路径
        edgeMapPath = new HashMap<>();
        for (int vertexStart : vertexSet) {
            // 记录已经到达的点
            Set<Integer> reached = new HashSet<>();
            // 存储从单个点开始的所有路径
            Queue<ArrayList<Integer>> path = new LinkedList<>();
            Queue<Integer> queue = new LinkedList<>();
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(vertexStart);
            path.offer(temp);
            queue.offer(vertexStart);
            while (!queue.isEmpty()) {
                int tempVertex = queue.poll();
                ArrayList<Integer> tempPath = path.poll();
                if ((pathRecordMap.get(tempVertex) == null || reached.contains(tempVertex))) {
                    if (Objects.requireNonNull(tempPath).size() == pathLen) {
                        allPaths.add(tempPath);
                        // 遍历路径的所有边，在HashMap中以边为键映射到边
                        for (int i = 0; i < tempPath.size() - 1; i++) {
                            int k = tempPath.get(i);
                            int v = tempPath.get(i + 1);
                            int edgeId = vertexPairMapEdge.get(Map.entry(k, v));
                            if (edgeMapPath.containsKey(edgeId)) {
                                ArrayList<ArrayList<Integer>> tmpPathList = edgeMapPath.get(edgeId);
                                tmpPathList.add(tempPath);
                            } else {
                                ArrayList<ArrayList<Integer>> tempPathList = new ArrayList<>();
                                tempPathList.add(tempPath);
                                edgeMapPath.put(edgeId, tempPathList);
                            }
                        }
                    }
                } else {
                    ArrayList<Integer> tmpArr = pathRecordMap.get(tempVertex);
                    for (Integer integer : tmpArr) {
                        if (integer != vertexStart) {
                            if (vertexType[integer] == vertexType[vertexStart])
                                reached.add(integer);
                            queue.offer(integer);
                            assert tempPath != null;
                            ArrayList<Integer> newArr = new ArrayList<>(tempPath);
                            newArr.add(integer);
                            path.offer(newArr);
                        }
                    }
                }
            }
        }
    }

    private void recordPath(HashMap<Integer, ArrayList<Integer>> pathRecordMap, int anchorId, int nbVertexId) {
        if (!pathRecordMap.containsKey(anchorId)) {
            ArrayList<Integer> vertexLinker = new ArrayList<>();
            vertexLinker.add(nbVertexId);
            pathRecordMap.put(anchorId, vertexLinker);
        } else {
            ArrayList<Integer> vertexLinkerPointer = pathRecordMap.get(anchorId);
            vertexLinkerPointer.add(nbVertexId);
        }
    }
}