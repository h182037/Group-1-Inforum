# INFORUM – The INsecure FORUM

Welcome to this third mandatory assignment of INF226.
In this assignment you will be improving the security
of a program called Inforum – a very simple discussion
forum in the form of a web-application.

Inforum has been especially crafted to contain a number
of security flaws. You can imagine that it has been
programmed by a less competent collegue, and that after
numerous securiy incidents, your organisation has decided
that you – a competent security professional – should take
some time to secure the app.

For your convenience, the task is separated into specific
exercises or tasks. These task might have been the result
of a security analysis – like the one you had in the
second assignment. If you discover any security issues
beyond these tasks, you can make a note of them at the
end of this report.

For each task, you should make a short note how you solved
it – ideally with a reference to the relevant git-commits you
have made.

## Evaluation

This assigment is mandatory for the course, and counts 10%
of your final grade. The assigment is graded 0–10 points,
where you must get a minimum of 3 points in order to pass
the assignment.

## Handing in the assignment

Before you hand in your assignment, make sure that you
have included all dependencies in the file `pom.xml`, and
that your program compiles and runs well.

Once you are done, you submit the assignment on
[`mitt.uib.no`](https://mitt.uib.no/) as a link to your
repository. This means you should not commit to the
repository after the deadline has passed.

## Updates

Most likely the source code of the project will be updated
while you are working on it. Therefore, it will be part of
your assignment to merge any new commits into your own branch.

## Improvements?

Have you found a non-security related bug – maybe you have
an idea for a new feature you want to add to the forum?
Feel free to open an issue on the project GitLab page.
The best way is to make a separate git branch for these
changes, which do not contain your sulutions 

(This is ofcourse completely volountary – and not a graded
part of the assignment)

## Tasks

The tasks below has been separated out, for you

### Task 0 – Authentication

The original authentication mechanisms of Inforum was so insecure it had to be removed
immediately and all traces of the old passwords have been purged
from the database. Therefore, the code in `inf226.inforum.User`, which is
supposed to check the password, always returns `true`.

Update the code to use a secure password authentication method in `User.checkPassword` – one
of the secure methods we have discussed
in lecture.

Any data you need to store for the password check can be kept in the `User` class, with
appropriate updates to `storage.UserStorage`. Remember that the `User` class is *immutable*.
Any new field must be immutable and `final`
as well.

Additionally, while the session cookie is an unguessable UUID, you must set the
correct protection flags on the session cookie.

#### Notes – task 0

Here you write your notes about how this task was performed.

### Task 1 – SQL injection

#### Notes – task 1

Here you write your notes about how this task was performed.

### Task 2 – Cross-site scripting


#### Notes – task 2

Here you write your notes about how this task was performed.


### Task 3 – Cross-site request forgery

#### Notes – task 3

Here you write your notes about how this task was performed.


### Task 4 – Access control

Inforum has no access control on operations such as *deleting a message*,
or *posting a new message*.

 - Identify which actions need access control, and decide
   on an access control model.
 - Implement your access control model.

#### Notes – task 4

Here you write your notes about how this task was performed.


### Task ω – Other security holes

There are more security issues in this web application.
If you find any of them, improve the source code and
explain below.

#### Notes – task ω

Here you write your notes about how this task was performed.
