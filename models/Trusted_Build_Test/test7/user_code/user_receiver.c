#include <smaccm_receiver.h>

void periodic_ping(/* const */uint32_t periodic_1000_ms) {
	uint32_t test_data; 
	
	printf("receiver: periodic dispatch received at time: %d", periodic_1000_ms); 
    printf("receiver: checking value of test_data on Input1 \n");
    ping_Input1(&test_data);
    printf("receiver: test_data value: (%d)\n", test_data);
}