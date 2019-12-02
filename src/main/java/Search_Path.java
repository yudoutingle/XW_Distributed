

public class Search_Path {
    public final static int MaxValue=Integer.MAX_VALUE;
    public final static int init_value=1000;
    public static void main(String[] args) {
        long begin=System.currentTimeMillis();
        double G[][]=initG(1000,0.0);
        System.out.println(G[1][100]);
        G[10][100]=0.005;
        G[100][999]=0.005;
        G[10][999]=0.1;
        int path[]=getShortedPath(G,10,999);
        long end=System.currentTimeMillis();
        System.out.println(end-begin);
        System.out.println(get_next(G,10,999));
    }
    public static  double[][] add_adj(double G[][],int from,int to,double lag){
        G[from][to]=lag;
        G[to][from]=lag;
        return G;
    }
    
    public static  void remove(double G[][],int from,int to){
        G[from][to]=init_value;
        G[to][from]=init_value;
    }
    
    public static double[][] initG(int len,double lag){
        double G[][] =new double[len+1][len+1];
        for(int i=0;i<len+1;i++){
            for(int j=0;j<len+1;j++){
                G[i][j]=init_value;
            }
        }
//        for(int i=1;i<len+1;i++){
//            if(i/2>0){
//                G[i][i/2]=lag;
//            }
//            if(i*2<len+1){
//                G[i][i*2]=lag;
//            }
//            if(i*2+1<len+1){
//                G[i][i*2+1]=lag;
//            }
//        }
        return G;
    }
    public static int get_next(double [][] G, int start, int end){
        int path[]=getShortedPath(G,start,end);
        if(path.length<2){
            return path[0];
        }
        return path[1];
    }
    public static int[] getShortedPath(double [][] G, int start, int end)
    {

        int length=G.length;
        int path[]=new int [length];
        for(int i=0;i<length;i++){
            path[i]=-1;
        }
        boolean [] s = new boolean[length];
        double min;
        int curNode = 0;
        double[] dist = new double[length];
        int[] prev = new int[length];
        for (int v = 0; v < length; v++)
        {
            s[v] = false;
            dist[v] = G[start][ v];
            if (dist[v] > MaxValue)
                prev[v] =-1;
            else
                prev[v] = start;
        }
        path[0] = end;
        dist[start] = 0;
        s[start] = true;
        for (int i = 1; i < length; i++)
        {
            min = MaxValue;
            for (int w = 0; w < length; w++)
            {
                if (!s[w] && dist[w] < min)
                {
                    curNode = w;
                    min = dist[w];
                }
            }
            s[curNode] = true;
            for (int j = 0; j < length; j++)
                if (!s[j] && min + G[curNode][ j] < dist[j])
                {
                    dist[j] = min + G[curNode][j];
                    prev[j] = curNode;
                }
        }
        int e = end, step = 0;
        while (e != start)
        {
            step++;
            path[step] = prev[e];
            e = prev[e];
            if(e<0){
                return new int[1];
            }
        }
        for (int i = step; i > step / 2; i--)
        {
            int temp = path[step - i];
            path[step - i] = path[i];
            path[i] = temp;
        }
        int count=0;
        for(int i=0;i<length;i++){
            if(path[i]!=-1){
                count++;
            }else{
                break;
            }
        }
        int result[]=new int[count];
        for(int i=0;i<count;i++){
            result[i]=path[i];
        }
        return result;
    }
}
