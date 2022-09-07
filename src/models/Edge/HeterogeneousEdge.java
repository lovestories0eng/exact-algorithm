package models.Edge;


/**异构图中的边**/
public class HeterogeneousEdge {
    // 边能够被共享的次数
    public int capacity;
    // 边已经被共享的次数，用于残差图
    public int flow;
    public int startPoint;
    public int endPoint;
    // 多部图中，边的方向，true代表正向，false代表反向
    public boolean direction;

    public HeterogeneousEdge(int capacity) {
        // 初始化边容量
        this.capacity = capacity;
        // 初始化边流量为零
        this.flow = 0;
    }

    public void swap() {
        int tmp;
        tmp = this.startPoint;
        this.startPoint = this.endPoint;
        this.endPoint = tmp;
    }

    public int getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(int nodeId) {
        this.startPoint = nodeId;
    }

    public void setEndPoint(int nodeId) {
        this.endPoint = nodeId;
    }

    public int getEndPoint() {
        return endPoint;
    }
}
