package binil.jersey.client.experiment;

public class ProgressIndicator {
    final int breakAfterNDots;

    int counter;

    ProgressIndicator(int breakAfterNDots) {
        this.breakAfterNDots = breakAfterNDots;
    }

    public synchronized void tick() {
        System.out.print(".");
        counter++;
        if (counter >= breakAfterNDots) {
            System.out.println("");
            counter = 0;
        }
    }
}
