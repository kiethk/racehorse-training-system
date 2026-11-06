# Training

How a Trainer organises racehorses into fixed groups and runs them through courses on the track each morning.

## Language

**Training Group**:
A named, lasting set of horses (at most 6) owned by one Trainer, going out on the track together on training days. A horse belongs to at most one Training Group at a time, and no two horses of the same Groom share one.
_Avoid_: Lot, cohort, batch

**Disbanded**:
The state of a Training Group its Trainer has closed for good: it keeps its name and its past Group Plans, but has no horses, takes no new horses and gets no new Group Plan. Disbanding is always the Trainer's own act, never a result of the group running empty. Disbanding a group that still has unfinished Group Plans cancels them in the same act. A group that never had a Group Plan is simply removed, leaving no trace.
_Avoid_: Deleted, cancelled, archived

**Group Plan**:
A Training Group running one Course from a start date on chosen weekdays; it ends together for the whole group when its last Session finishes. Horses may join or leave while it runs.
_Avoid_: Horse plan, enrollment

**Session**:
One outing of a Training Group on one day, running one Subject, with a start time found for that day.
_Avoid_: Lot, class

**Workout**:
One horse's result within a Session.
_Avoid_: Session (that is the group's outing)

**Held**:
The state of a Session, and of the Workouts in it, once its end time has passed without being cancelled. It follows the clock, never whether results have been recorded. A Held Workout is not yet confirmed: it waits until its horse is shown to have Ran or marked Did not run, and stays waiting for as long as nobody does.
_Avoid_: Completed (that mixes "took place" with "result entered")

**Ran**:
The state of a Held Workout once its results are recorded; the recorded results are the proof the horse took part. Only the Trainer records them.
_Avoid_: Attended, present, completed

**Cancel**:
Withdrawing a Session, one horse's Workout, or a whole Group Plan before it begins; nothing is made up. Cancelling a single Workout keeps the horse in its Training Group and leaves its other Workouts alone.
_Avoid_: Skip, postpone

**Did not run**:
A mark set on a Held Workout whose horse did not actually take part; the Workout becomes cancelled. No reason is given: a horse unfit to train is blocked by the Vet instead.
_Avoid_: No-show, absent

**Golden Window**:
The morning hours, 06:00 to 10:00, in which every Session must start and end; Session start times fall on 15-minute steps.
_Avoid_: Training hours, time slot

**Attendance**:
How many of a Course's Sessions a horse took part in (its Workouts that Ran), shown against the Course total (for example 7 of 20). A missed Session is never made up: the horse simply goes on with its group.

**Course**:
An ordered list of Subjects that a Training Group works through over a number of Sessions.

**Subject**:
A single exercise (distance, surface, intensity, duration) run in a Session.

### Incidents

**Incident Report**:
A notice from a horse's Groom or Trainer that a horse in a stall has a health problem, rated with a Severity.
_Avoid_: Ticket, complaint

**Severity**:
How serious the reporter judges an Incident Report to be. Low and Medium wait for the next routine vet slot and the horse keeps training; High and Critical stop training and call a vet at once, even from another examination.
_Avoid_: Priority

**Incident Case**:
The one open examination that handles every Incident Report of a horse. A horse has at most one, and a High or Critical report replaces a routine Incident Case that has not started.
_Avoid_: Urgent case (only a High or Critical one is urgent)

**Follow-up**:
An examination booked to see how a horse is progressing and decide whether it may train. A horse has at most one waiting; a visit opened by an Incident Report is not a Follow-up.
_Avoid_: Recheck, Re-examination

**Closed**:
The end of an Incident Report whose Incident Case was cancelled, so no vet examined it; the reason stays on the report.
_Avoid_: Dismissed, No issue

### Admissions

**Quarantine Groom**:
A Groom who looks after at least one stall in a quarantine area. Only Quarantine Grooms can be given an Admission to review.

**Assigned Groom**:
The one Quarantine Groom an Admission in Groom review is given to. Only the Assigned Groom sees and decides it, and the same Groom keeps the horse in one of their own quarantine stalls when it arrives. An Admission is assigned even when every quarantine stall is taken, but it can only be approved once the Assigned Groom has a free quarantine stall.
_Avoid_: Reviewer pool, any quarantine groom

**Full**:
A Quarantine Groom with no free quarantine stall left. A Full Groom can still reject an Admission but cannot approve one. When an approval makes a Groom Full, their Admissions still waiting for a decision are reassigned to the Quarantine Groom with a free stall and the lowest Groom Workload; if every Groom is Full they stay where they are.

**Groom Workload**:
The number of horses in a Groom's stalls plus the Admissions assigned to that Groom that are still waiting for the Groom's decision. The lower it is, the sooner the Groom gets the next Admission. On a tie, the Groom who last confirmed a horse's arrival longest ago (or never) goes first.
_Avoid_: Load, capacity

**Reassignment**:
Handing an Admission in Groom review from its Assigned Groom to the Quarantine Groom with the next lowest Groom Workload who has not had it yet, because the Assigned Groom did not decide within 72 hours of being assigned, became Full, was deactivated or no longer looks after a quarantine stall. The 72 hours are counted around the clock and restart with every Reassignment. Once every Quarantine Groom has had it, the rotation starts again from the lowest Groom Workload. The Manager can also reassign an Admission by hand to any Quarantine Groom. The new Assigned Groom, the previous one and the Manager are told every time.
_Avoid_: Escalation, transfer

**Review Due**:
The moment, 72 hours after the latest assignment, when an Admission still waiting for its Assigned Groom's decision is reassigned. Shown to the Groom and the Manager.
_Avoid_: Deadline, SLA
