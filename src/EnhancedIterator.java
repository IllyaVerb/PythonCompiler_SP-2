import java.util.ArrayList;
import java.util.Iterator;

public class EnhancedIterator<E> implements Iterator<E> {
    private ArrayList<E> list;
    private int indexSelected=-1;
    public EnhancedIterator(ArrayList<E> list){
        this.list=list;
    }

    @Override
    public boolean hasNext() {
        return indexSelected<list.size()-1;
    }

    @Override
    public E next() {
        indexSelected++;
        return current();
    }

    @Override
    public void remove() {
        list.remove(indexSelected);
    }
    public void remove(int i){
        list.remove(i);
        if(i<indexSelected){
            indexSelected--;
        }
    }
    public E previous(){
        indexSelected--;
        return current();
    }
    public E current(){
        return list.get(indexSelected);
    }
    public E peek(){
        return list.get(indexSelected+1);
    }
    public E get(int i){
        return list.get(i);
    }
}