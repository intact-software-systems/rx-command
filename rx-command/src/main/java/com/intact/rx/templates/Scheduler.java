package com.intact.rx.templates;

public class Scheduler { // implements Runnable {

//    @Override
//    public void run() {}

    /**
     * HoursOfOperation or OpeningHours: Policy on when system is open, family of circuit breaker
     * <p>
     * Note: May be replaced by lambda () -> isOpenForBusiness()
     */
    private static class HoursOfOperation {
        // Times per day
        // Dates of the year
        // Days of the week
    }

    /*

      Frequency: (i.e., the number of occurrences of a repeating event per unit time) Policy on a job (submission or dialogue type)

      HoursOfOperation or OpeningHours: Policy on when system is open, family of circuit breaker

      RateLimiter:
       - Limit the system as a whole to operate below a frequency policy, family of circuit breaker

      DropPattern:
        - Drop and reject requests with increasing probability when queue size grows beyond a threshold.

      Planning (validation):
       - Upon jobs (submissions) entering system:
           - ensure cumulative frequency policies on all current submissions plus this new one is below system rate limit

      Scheduler:
       - Hand out jobs based on requests (pull based), not faster then job frequency policies and RateLimiter for system
       - Enforce Hours of Operation policy (opening hours)


      Validator: state checker, timeouts, failures, etc:
       - Continuously check job cache (db) and statuses (timestamps, counters, etc)
       - If timeout, then retry and/or log
       - If failure, then retry and/or log
       - Alert if subscribers (notifiers) are too slow - logging


      Data:
      - Job cache (db) of distributed jobs: notifications and status
      - Schedule cache (db) of distributed jobs with status

     */

    private static class Worker {}

    private void request(Worker worker, int num) {
        // if available work, then
    }


        /*
     * Challenges with drain queue or "earliest pickup time"
     * - After a Worker picks up a sub-job, the sub-job should contain:
     * - the rate of dispatch.
     * - outstanding work ready to be picked up (for immediate return to pickup more)
     * - when is n items ready to be picked up (for worker self scheduling)
     *
     * - Each Worker
     * - Check MaxSystemRate and only drains such that the policy is enforced
     * - Check DispatchFrequency for a dialogue
     *
     *
     * Return a SubJob:
     * - list of ScheduleNotifications
     * - Policies on dispatch: Rate (frequency or interval), ASAP -> SLOW,
     * - Current status of queues:
     *      - outstanding work ready to be picked up (for immediate return to pickup more)
     *      - when is n items ready to be picked up (for worker self scheduling)
     * - Next pull scheduling
     */

    private void addWork() {

    }


    private static class WorkRequest {
        //Worker worker;
        //int num;
    }


    private static class AtomicWeakLoan<T> {

        // Reference is a WeakReference
        //  - As long as someone has a reference to T then it is accessible
        //  - Refresh only when reference is invalid
        // Consequence:
        // - Consistent view of T
        // -
        //
        //
    }
}
