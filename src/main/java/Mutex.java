public class Mutex{
    private volatile boolean isLocked = false;

    public synchronized void lock(){
        while (isLocked){
            try {
                wait();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        isLocked = true;
    }
    public synchronized void unlock(){
        isLocked = false;
        notify();
    }
    public synchronized void unlock(int delay){
        try {
            wait(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isLocked = false;
        notify();
    }

    public boolean isLocked() {
        return isLocked;
    }
}
