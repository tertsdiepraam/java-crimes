package first;

record Connection (RemoteClient c, Message m, int delay) implements Runnable {
    public void run() {
        try {
            Thread.sleep(delay);
            c.receive(m);
        } catch (Exception e) {}
    }
}
