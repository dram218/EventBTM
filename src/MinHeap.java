import java.util.ArrayList;

public class MinHeap {	 
	public DocTopicDistribution[]   array;
	public int size;
	public int capacity;
	
	public MinHeap(int beamsize)
	{
		array = new DocTopicDistribution[beamsize];
		size=0;
		capacity=beamsize;
	}
	
	
	
    public void Sort() {
        if (array == null) {
             throw new NullPointerException();
        }
                 
        for (int i = size-1; i >= 0; i--) {
             swap(0, i);
             minHeapify(0, i);
        }  
   }
    
    public void Sort(int n) {
         if (array == null) {
              throw new NullPointerException();
         }
         if (n > size) {
              throw new ArrayIndexOutOfBoundsException();
         }
          
         buildMinHeap(n);
          
         for (int i = n-1; i >= 1; i--) {
              swap(0, i);
              minHeapify( 0, i);
         }
    }
     

    /**
     * ����һ��С����;
     * @param a
     * @param n
     */
    public void buildMinHeap(int n){  
    	 for (int i = n/2-1 ; i >= 0; i--) {
             minHeapify(i, n);
        }
    }
   
    public void Add(DocTopicDistribution b)
    {
    	if(b.prob > array[0].prob)
    	{
    	    array[0] = b;
    	    buildMinHeap(size);
    	}
    }
    
    public void AddNew(DocTopicDistribution a){
    	if(size<capacity){
	    	array[size]=a;
	    	size++;
	    	buildMinHeap(size);
    	}
    	else{
    		Add(a);
    	}
    }
    
    private void minHeapify(int i, int n) {
    	int smallest;
        
        int leftIndex = 2 * i + 1;
        int rightIndex = leftIndex + 1;
         
        if (leftIndex < n && array[leftIndex].prob < array[i].prob) {
        	smallest = leftIndex;
        } else {
        	smallest = i;
        }
        if (rightIndex < n && array[rightIndex].prob < array[smallest].prob) {
        	smallest = rightIndex;
        }
         
        if (smallest != i) {
             swap(i, smallest);
             minHeapify(smallest, n);
        }
    	
    	
    }
    

    private void swap(int pX, int pY) {
    	DocTopicDistribution temp = array[pX];
    	array[pX] = array[pY];
    	array[pY] = temp;
    }
}