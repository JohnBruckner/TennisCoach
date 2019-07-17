package uk.ed.ac.specknet.tenniscoach;

public interface AsyncResponse {
    void classificationFinish(Integer classification);
    void ratingFinish(Double result);
}
