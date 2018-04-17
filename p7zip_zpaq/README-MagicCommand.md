MagicCommand
=================================

# Introduction

**MagicCommand** is a Java library which is used for running any programs by commands.

# Usage

## Command Class

**Command** class is in the *org.magiclen.magiccommand* package. It can help you run system commands in Java.

### Initialize

You have to input your command to construct **Command** object. For example, if you want to `ping` some host IP, such as `8.8.8.8` for Google Public DNS. You can write the code below,

    final String commandString = "ping 8.8.8.8";
    final Command command = new Command(commandString);

### Run command

You can use **run** method or **runAsync** method to run the command in your **Command** object. Certainly, **run** method is
synchronous and **runAsync** method is asynchronous. One **Command** object can make more than 1 command tasks(processes). Each of them has an unique task ID to recognize. For example, run an asynchronous command task and set its ID to `first`.

    command.runAsync("first");

### Monitor your command tasks

You can use **setCommandListener** method the add your listener to monitor your command tasks before you run all of the command tasks.

    command.setCommandListener(new CommandListener() {

    	    @Override
    	    public void commandStart(final String id) {
    	    }

    	    @Override
    	    public void commandRunning(final String id, final String message, final boolean isError) {
    		      System.out.println(message);
    	    }

    	    @Override
    	    public void commandException(final String id, final Exception exception) {
    		      exception.printStackTrace(System.out);
    	    }

    	    @Override
    	    public void commandEnd(final String id, final int returnValue) {
    	    }
    	});

### Input data to your command task

You can input string data to any command task found by task ID when it is running. For example, input `exit` text to the command task whose ID is `first`,

    command.inputStringToRunningProcess("first", "exit\n");

### Stop your command task

To stop a command task found by task ID `first`,

    command.stop("first");

To stop all the command tasks,

    command.stopAll();

### Send a signal to process(es) specified by process ID(PID)

To send a signal to one process or more than one processes specified by PID, just use the static method `sendSignal`. For example,

    Command.sendSignal(Command.Signal.SIGINT, pids);

# License

    Copyright 2015-2017 magiclen.org

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

# What's More?

Please check out our web page at

https://magiclen.org/java-command-executable/
