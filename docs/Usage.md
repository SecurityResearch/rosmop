Usage:
-One event specification (.rv) file
rosmop a.rv

-Multiple event specification (.rv) files
rosmop a.rv b.rv ... z.rv

-Folder of *only* event specification (.rv) files
rosmop specs/


Event specification:
#include <library>
spec(){
	int i;
	bool b;

	event event1(parameters) topic messageType '{pattern}'
	{
		//action code
	}
}


Event specification names are used to identify the monitors. By using those
names, one can enable or disable desired monitors, and hence control which
events take place. One shall create different specifications for each separate
concern, so that disabling a monitor does not interfere with the functioning of
others.

In an event specification, there can be multiple events and the user specified
action codes of these different events can communicate through adding
specification-scoped shared variables. This allows the user to monitor
properties across different topics.  One should be careful when generating the
code from multiple specifications as all the shared variables become global for
all the callback functions. **For now, it is the user's responsibility to mind
the possible duplicates as well as making sure that the action code is
compilable.**

In case of multiple events on one topic, the callback function is named after
the topic and the action codes of these events are merged. However, this does
not affect the ability to enable/disable different monitors with events on the
same topic.

The user uses the parameters (s)he specified in the action code to check or
(possibly) alter the information received in the message. The event parameters
(along with their types) and the variable matching in the specified pattern
should be compatible with the given message type. Furthermore, the variable
names of the parameters and the ones in the pattern should match each other. If
there is such an invalid matching, the code will not be generated correctly. 

The following is an example for an event specification:

velocity(){
	bool firstSlow = true;

	event twoTimesSlower(double lx, double ly, double lz, double ax, double ay, double az) /landshark_control/base_velocity geometry_msgs/TwistStamped '{twist:{angular:{x:ax,y:ay,z:az},linear:{x:lx,y:ly,z:lz}}}'
       {
         lx = lx/2;
         ly = ly/2;
         lz = lz/2;
         ax = ax/2;
         ay = ay/2;
         az = az/2;
		 
		 if(firstSlow){
			ROS_INFO("Going 2x slower now");
			firstSlow = false;
		 }
       }
}
