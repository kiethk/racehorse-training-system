# Training

How a Trainer organises racehorses into fixed groups and runs them through courses on the track each morning.

## Language

**Training Group**:
A named, lasting set of horses (at most 6) owned by one Trainer, going out on the track together on training days. A horse belongs to at most one Training Group at a time, and no two horses of the same Groom share one.
_Avoid_: Lot, cohort, batch

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
The state of a Session, and of the Workouts in it, once its end time has passed without being cancelled. It follows the clock, never whether results have been recorded.
_Avoid_: Completed (that mixes "took place" with "result entered")

**Cancel**:
Withdrawing a Session, one horse's Workout, or a whole Group Plan before it begins; nothing is made up. Cancelling a single Workout keeps the horse in its Training Group and leaves its other Workouts alone.
_Avoid_: Skip, postpone

**Did not run**:
A mark set while recording results on a Held Workout whose horse did not actually take part; the Workout becomes cancelled and no longer counts in Attendance.
_Avoid_: No-show, absent

**Golden Window**:
The morning hours, 06:00 to 10:00, in which every Session must start and end; Session start times fall on 15-minute steps.
_Avoid_: Training hours, time slot

**Attendance**:
How many of a Course's Sessions a horse took part in (its Held Workouts), shown against the Course total (for example 7 of 20).

**Course**:
An ordered list of Subjects that a Training Group works through over a number of Sessions.

**Subject**:
A single exercise (distance, surface, intensity, duration) run in a Session.
