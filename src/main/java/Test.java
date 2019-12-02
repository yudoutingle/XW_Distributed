import java.util.Arrays;

public class Test {

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int N = 40;

		
		for(int i = 0; i< N; i++) {
			try {
				Demo d = new Demo();
				Thread t = new Thread(d);
				t.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	

}
