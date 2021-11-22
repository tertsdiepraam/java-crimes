package first;

record Connection (RemoteClient c, Message m) implements Runnable {
    public void run() {
        int delay = (int) (Math.random() * 2000);
        try {
            Thread.sleep(delay);
            c.receive(m);
        } catch (Exception e) {}
    }
}
